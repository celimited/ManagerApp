package org.celimited.manager.data.local.attendance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.celimited.manager.model.attendance.AttendanceSessionData
import org.celimited.manager.model.attendance.AttendanceStatus
import org.celimited.manager.model.attendance.LocalCheckIn

/**
 * Session-scoped cache of the attendance popup data, so returning to Home reuses it instead of
 * calling the APIs again. In-memory like [org.celimited.manager.core.common.session.InMemoryUserSession];
 * swap for SQLDelight once it's added.
 */
class AttendanceLocalDataSource {
    private val _data = MutableStateFlow<AttendanceSessionData?>(null)
    val data: StateFlow<AttendanceSessionData?> = _data.asStateFlow()

    private var autoPopupShown = false

    fun update(transform: (AttendanceSessionData) -> AttendanceSessionData) {
        _data.update { transform(it ?: AttendanceSessionData()) }
    }

    fun recordCheckIn(checkIn: LocalCheckIn) {
        _data.update { current ->
            val data = current ?: AttendanceSessionData()
            data.copy(
                todayAttendance = data.todayAttendance?.copy(status = AttendanceStatus.PRESENT),
                localCheckIn = checkIn
            )
        }
    }

    /** True only the first time it's called after [clear]. */
    fun consumeAutoPopup(): Boolean {
        if (autoPopupShown) return false
        autoPopupShown = true
        return true
    }

    fun clear() {
        _data.value = null
        autoPopupShown = false
    }
}
