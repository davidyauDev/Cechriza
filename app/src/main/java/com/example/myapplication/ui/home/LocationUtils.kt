package com.example.myapplication.ui.home

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.myapplication.data.local.dao.LocationDao
import com.example.myapplication.data.local.entity.LocationEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume


sealed class LocationResult {
    data class Success(val location: Location) : LocationResult()
    data class Error(val reason: LocationError) : LocationResult()
}

enum class LocationError {
    PERMISSION_DENIED,
    TIMEOUT,
    GPS_DISABLED,
    NO_LOCATION_AVAILABLE,
    INACCURATE,
    UNKNOWN
}

// ---------- Main function ----------

suspend fun awaitLocationForAttendanceImproved(
    client: FusedLocationProviderClient,
    context: Context,
    dao: LocationDao,
    timeoutMs: Long = 10000L,
    maxAgeMs: Long = 60_000L,
    minAccuracyMeters: Float = 120f,
): LocationResult {

    try {
        //  Permisos
        val fine = ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fine != PackageManager.PERMISSION_GRANTED &&
            coarse != PackageManager.PERMISSION_GRANTED
        ) {
            return LocationResult.Error(LocationError.PERMISSION_DENIED)
        }

        //  GPS / Network
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            return LocationResult.Error(LocationError.GPS_DISABLED)
        }

        val hasNetwork = isNetworkAvailable(context)

        val effectiveTimeout =
            if (hasNetwork) timeoutMs else timeoutMs * 2

        val effectiveMinAccuracy =
            if (hasNetwork) minAccuracyMeters else 400f






        val freshLocation = withTimeoutOrNull(effectiveTimeout) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cts = CancellationTokenSource()
                client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                )
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }

                cont.invokeOnCancellation { cts.cancel() }
            }
        }

        if (freshLocation != null) {

            val ageMs = System.currentTimeMillis() - freshLocation.time
            val isFresh = ageMs <= 30_000L

            val isAccurate =
                freshLocation.hasAccuracy() &&
                        freshLocation.accuracy <= effectiveMinAccuracy
            //Aceptar estrictamente si hay red
            if (isAccurate && isFresh) {
                saveToRoom(dao, freshLocation)
                return LocationResult.Success(freshLocation)
            }
            //SIN INTERNET → aceptar aunque sea imprecisa
            if (!hasNetwork && isFresh) {
                saveToRoom(dao, freshLocation)
                return LocationResult.Success(freshLocation)
            }
        }


        // Last known
        val lastKnown = withTimeoutOrNull(1000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }

        if (lastKnown != null) {
            val ageMs = System.currentTimeMillis() - lastKnown.time
            val isRecent = ageMs <= maxAgeMs
            val isAccurate =
                lastKnown.hasAccuracy() &&
                        lastKnown.accuracy <= minAccuracyMeters

            if (isRecent && isAccurate) {
                saveToRoom(dao, lastKnown)
                return LocationResult.Success(lastKnown)
            }
        }

        //  Room (último fallback)
        val stored = try {
            dao.getLastLocation()
        } catch (_: Exception) {
            null
        }

        if (stored != null) {
            val loc = Location("room").apply {
                latitude = stored.latitude
                longitude = stored.longitude
                accuracy = stored.accuracy
                time = stored.timestamp
            }
            return LocationResult.Success(loc)
        }

        return LocationResult.Error(LocationError.NO_LOCATION_AVAILABLE)

    } catch (_: SecurityException) {
        return LocationResult.Error(LocationError.PERMISSION_DENIED)
    } catch (_: Exception) {
        return LocationResult.Error(LocationError.UNKNOWN)
    }
}

// ---------- Helper ----------

private suspend fun saveToRoom(
    dao: LocationDao,
    location: Location
) {
    try {
        dao.insert(
            LocationEntity(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = if (location.hasAccuracy()) location.accuracy else 0f,
                timestamp = location.time
            )
        )
    } catch (_: Exception) {
        // opcional: log
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    val cm =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
