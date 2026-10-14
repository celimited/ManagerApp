package org.celimited.manager.feature.distributorList

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.celimited.manager.component.ErrorDialogUi

@Immutable
data class DistributorItemUi(
    val id: Int,
    val name: String,
    val code: String,
    val address: String
)

@Immutable
data class DistributorListUiState(
    /** Already filtered by [query]. */
    val distributors: ImmutableList<DistributorItemUi> = persistentListOf(),
    val query: String = "",
    val isRefreshing: Boolean = false,
    /** First load when nothing was cached at login. */
    val isInitialLoading: Boolean = false,
    /** API failure popup; null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null
)
