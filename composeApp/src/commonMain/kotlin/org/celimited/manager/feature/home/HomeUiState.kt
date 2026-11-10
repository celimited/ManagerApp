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
    val showLogoutConfirm: Boolean = false,
    val todayOrder: TodayOrderUi = TodayOrderUi(),
    val mtdPerformance: MtdPerformanceUi = MtdPerformanceUi()
)

private const val NO_VALUE = "--"

/** Home "Today's Order" cards, display-ready. "--" until the dashboard API answers. */
@Immutable
data class TodayOrderUi(
    val attendance: String = NO_VALUE,
    val orderStarted: String = NO_VALUE,
    val idleSr: String = NO_VALUE,
    val callProductivity: String = NO_VALUE,
    val visitProductivity: String = NO_VALUE,
    val lpc: String = NO_VALUE
)

/** Home "MTD Performance" block, display-ready. */
@Immutable
data class MtdPerformanceUi(
    /** 0–100, drives the progress bar. */
    val mtdAchievedProgress: Float = 0f,
    val mtdAchieved: String = NO_VALUE,
    val ytdAchievedProgress: Float = 0f,
    val ytdAchieved: String = NO_VALUE,
    val tillDateTarget: String = NO_VALUE,
    val mtdGrowth: GrowthUi = GrowthUi(),
    val ytdGrowth: GrowthUi = GrowthUi(),
    val m2mGrowth: GrowthUi = GrowthUi(),
    val strikeRate: String = NO_VALUE,
    val lpc: String = NO_VALUE,
    val mtdSalesValue: String = NO_VALUE,
    val ytdSalesValue: String = NO_VALUE,
    val orderVsDelivery: String = NO_VALUE,
    val coverage: String = NO_VALUE
)

/** A growth chip; negative growth shows a down arrow on a yellow chip. */
@Immutable
data class GrowthUi(
    val value: String = NO_VALUE,
    val isPositive: Boolean = true
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
