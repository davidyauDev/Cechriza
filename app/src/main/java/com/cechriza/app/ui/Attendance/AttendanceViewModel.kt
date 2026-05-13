package com.cechriza.app.ui.Attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.cechriza.app.data.local.entity.Attendance
import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.remote.dto.response.AttendanceReportEmployeeItem
import com.cechriza.app.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class AttendanceViewModel(
    application: Application,
    private val repository: AttendanceRepository
) : AndroidViewModel(application) {

    private val _reportUiState = MutableStateFlow(AttendanceReportUiState())
    val reportUiState: StateFlow<AttendanceReportUiState> = _reportUiState.asStateFlow()

    fun loadAttendanceReport(
        startDate: String? = null,
        endDate: String? = null,
        dates: List<String>? = null
    ) {
        val employeeId = SessionManager.userId
        if (employeeId == null || employeeId <= 0) {
            _reportUiState.value = AttendanceReportUiState(
                isLoading = false,
                errorMessage = "No se encontro user_id de sesion. Vuelve a iniciar sesion."
            )
            return
        }

        viewModelScope.launch {
            _reportUiState.value = AttendanceReportUiState(isLoading = true)
            val result = repository.getAttendanceReportByEmployee(
                employeeId = employeeId,
                startDate = startDate,
                endDate = endDate,
                dates = dates
            )

            _reportUiState.value = result.fold(
                onSuccess = { records ->
                    AttendanceReportUiState(
                        isLoading = false,
                        records = records
                    )
                },
                onFailure = { error ->
                    AttendanceReportUiState(
                        isLoading = false,
                        errorMessage = toFriendlyAttendanceError(error)
                    )
                }
            )
        }
    }

    fun getLastAttendance(): LiveData<Attendance?> {
        return repository.getLastAttendance()
    }

    fun getAttendancesOfToday(): LiveData<List<Attendance>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis - 1

        return repository.getAttendancesBetween(startOfDay, endOfDay)
    }

    // New: expose repository helpers so the UI can request different date ranges or all attendances
    fun getAttendancesBetween(start: Long, end: Long): LiveData<List<Attendance>> =
        repository.getAttendancesBetween(start, end)

    fun getAllAttendances(): LiveData<List<Attendance>> =
        repository.getAllAttendances()

}

data class AttendanceReportUiState(
    val isLoading: Boolean = false,
    val records: List<AttendanceReportEmployeeItem> = emptyList(),
    val errorMessage: String? = null
)

private fun toFriendlyAttendanceError(error: Throwable): String {
    val raw = error.message.orEmpty().lowercase()
    return when {
        "unable to resolve host" in raw ||
            "failed to connect" in raw ||
            "timeout" in raw ||
            "socket" in raw -> "Sin conexion. Verifica tu internet e intenta nuevamente."
        "401" in raw || "403" in raw -> "Tu sesion vencio. Vuelve a iniciar sesion."
        else -> "No se pudo cargar la asistencia. Intenta nuevamente."
    }
}
