package org.celimited.manager.feature.attendance

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.datetime.LocalDate
import org.celimited.manager.component.DropdownOption
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.model.attendance.DayAttendanceStatus
import org.celimited.manager.utils.currentMonthNumber
import org.celimited.manager.utils.currentYearNumber

@Immutable
data class AttendanceLeaveUiState(
    val year: Int = currentYearNumber(),
    /** 1–12. */
    val month: Int = currentMonthNumber(),
    /** False on the current month — future months have no attendance yet. */
    val canGoNext: Boolean = false,
    val isLoading: Boolean = false,
    /** API failure popup; null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null,
    val summary: MonthlySummaryUi = MonthlySummaryUi(),
    /** Keyed by day of month. */
    val days: ImmutableMap<Int, DayAttendanceStatus> = persistentMapOf(),
    /** Null when the Apply Leave sheet is closed. */
    val applyLeave: ApplyLeaveFormUi? = null,
    val leaveResult: LeaveResultUi? = null
)

@Immutable
data class ApplyLeaveFormUi(
    val reasons: ImmutableList<DropdownOption> = persistentListOf(),
    val isLoadingReasons: Boolean = false,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val reasonId: Int? = null,
    val remarks: String = "",
    val isSubmitting: Boolean = false,
    /** Inline validation message; API failures go to [AttendanceLeaveUiState.errorDialog]. */
    val errorMessage: String? = null
)

/** Popup shown after an apply-leave call. */
@Immutable
data class LeaveResultUi(
    val isSuccess: Boolean,
    val message: String
)

@Immutable
data class MonthlySummaryUi(
    val percentage: Int = 0,
    val present: Int = 0,
    val late: Int = 0,
    val leave: Int = 0,
    val absent: Int = 0
)
