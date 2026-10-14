package org.celimited.manager.feature.teamAttendance

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.model.attendance.TeamMemberStatus

@Immutable
data class TeamAttendanceUiState(
    val levels: ImmutableList<HierarchyLevelTabUi> = persistentListOf(),
    /** `levelKey` of the selected tab; null until the levels load. */
    val selectedLevelKey: String? = null,
    val isLoadingLevels: Boolean = false,
    /** Counts for the selected level. */
    val summary: TeamSummaryUi = TeamSummaryUi(),
    val managers: ImmutableList<ManagerItemUi> = persistentListOf(),
    val isLoadingSummary: Boolean = false,
    /** API failure popup; null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null
)

@Immutable
data class HierarchyLevelTabUi(
    val key: String,
    /** Plain level name, e.g. "Division". */
    val name: String,
    /** "Division\n(2/20)". */
    val label: String
)

@Immutable
data class TeamSummaryUi(
    val total: Int = 0,
    val present: Int = 0,
    val late: Int = 0,
    val leave: Int = 0,
    val absent: Int = 0
)

@Immutable
data class ManagerItemUi(
    val userId: Int,
    val name: String,
    /** Up to two letters for the avatar; empty when the name has none. */
    val initials: String,
    /** Blank when the API has none — the row is then hidden. */
    val designation: String,
    val location: String,
    val status: TeamMemberStatus,
    /** "09:05 AM"; null when not checked in. */
    val checkInTime: String?
)
