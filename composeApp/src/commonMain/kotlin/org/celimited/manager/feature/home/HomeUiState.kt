package org.celimited.manager.feature.home

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.celimited.manager.component.DropdownOption
import org.celimited.manager.component.ErrorDialogUi

@Immutable
data class HomeUiState(
    val userName: String = "",
    val userRole: String = "",
    val attendancePopup: AttendancePopupUi = AttendancePopupUi.None,
    val attendanceCard: AttendanceCardUi = AttendanceCardUi.Unknown,
    /** API failure popup; null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null,
    val showLogoutConfirm: Boolean = false
)

/** State of the Home "Attendance" card. */
@Immutable
sealed interface AttendanceCardUi {
    data object Unknown : AttendanceCardUi
    data object NotGiven : AttendanceCardUi
    data object TimePassed : AttendanceCardUi
    /** [time] ("9:12 AM") and [isLate] are null when the check-in wasn't made on this device. */
    data class CheckedIn(val time: String?, val isLate: Boolean?) : AttendanceCardUi
    data object LeavePending : AttendanceCardUi
    data object OnLeave : AttendanceCardUi
}

@Immutable
sealed interface AttendancePopupUi {
    data object None : AttendancePopupUi

    /** "Mark Attendance" when [isLate] is false, "You're Late !" when true. */
    data class CheckIn(
        val isLate: Boolean,
        val minutesRemaining: Long?,
        val form: AttendanceFormUi,
        val geofenceWarning: GeofenceWarningUi? = null
    ) : AttendancePopupUi

    /** "On Leave" when [isApproved], otherwise "Leave Pending". */
    data class Leave(
        val isApproved: Boolean,
        val fromDate: String,
        val toDate: String
    ) : AttendancePopupUi

    data object WrongDeviceTime : AttendancePopupUi

    data class Success(val message: String) : AttendancePopupUi
}

@Immutable
data class AttendanceFormUi(
    val distributors: ImmutableList<DropdownOption> = persistentListOf(),
    val lateReasons: ImmutableList<DropdownOption> = persistentListOf(),
    val selectedDistributorId: Int? = null,
    val selectedReasonId: Int? = null,
    val remarks: String = "",
    val isSubmitting: Boolean = false,
    /** Inline validation / geofence message; API failures go to [HomeUiState.errorDialog]. */
    val errorMessage: String? = null
)

@Immutable
data class GeofenceWarningUi(
    val distanceMeters: Int,
    val radiusMeters: Int
)
