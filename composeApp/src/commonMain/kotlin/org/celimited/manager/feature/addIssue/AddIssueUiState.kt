package org.celimited.manager.feature.addIssue

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import org.celimited.manager.component.DropdownOption
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.model.distributor.IssueCategory
import org.celimited.manager.model.distributor.IssuePriority

const val ISSUE_TITLE_MAX_LENGTH = 150
const val ISSUE_DESCRIPTION_MAX_LENGTH = 1000

@Immutable
data class AddIssueUiState(
    val title: String = "",
    val category: IssueCategory = IssueCategory.General,
    val priority: IssuePriority = IssuePriority.Medium,
    /** Dropdown ids are team member user ids. */
    val teamMembers: ImmutableList<DropdownOption> = persistentListOf(),
    val isLoadingTeamMembers: Boolean = false,
    val assigneeId: Int? = null,
    val deadline: LocalDate? = null,
    val description: String = "",
    val isSubmitting: Boolean = false,
    /** Inline validation message; API failures go to [errorDialog]. */
    val errorMessage: String? = null,
    val result: IssueResultUi? = null,
    /** API failure popup; null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null
)

/** Popup shown after a save-issue call. */
@Immutable
data class IssueResultUi(
    val isSuccess: Boolean,
    val message: String
)
