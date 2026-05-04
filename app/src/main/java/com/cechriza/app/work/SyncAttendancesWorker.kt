package com.cechriza.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cechriza.app.data.local.database.AttendanceDatabase
import com.cechriza.app.data.preferences.UserPreferences
import com.cechriza.app.data.repository.AttendanceRepository

class SyncAttendancesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userPreferences = UserPreferences(applicationContext)
            val db = AttendanceDatabase.getDatabase(applicationContext)
            val dao = db.attendanceDao()
            val repo = AttendanceRepository(userPreferences, applicationContext, dao)

            val results = repo.syncUnsyncedAttendances()

            if (results.any { !it.second }) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

