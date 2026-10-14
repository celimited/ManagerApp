package org.celimited.manager.feature.todayStats

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.celimited.manager.component.ErrorDialogUi

@Immutable
data class SrProfileUi(
    val name: String,
    val designation: String,
    val location: String,
    val territory: String,
    val isOnline: Boolean,
    val phoneNo: String,
    val whatsAppUrl: String?
)

@Immutable
data class SrTimelineUi(
    val spent: String,
    val loggedInTime: String,
    val workMode: String,
    val visited: String,
    val firstOrderTime: String,
    val firstOrderOutlet: String,
    val lastOrderTime: String,
    val lastOrderOutlet: String
)

@Immutable
data class SrWorkUi(
    val scheduled: String,
    val visited: String,
    val productive: String,
    val lpc: String
)

@Immutable
data class SrTargetUi(
    val todayTarget: String,
    val orderValue: String,
    val achievementPercent: Int
)

@Immutable
data class SrOrderUi(
    val id: Int,
    val name: String,
    val subName: String,
    val qty: String,
    val memo: String,
    val specialDiscount: String,
    val promoDiscount: String,
    val totalDiscount: String,
    val freeValue: String,
    val orderQtyUom: String,
    val freeQty: String
)

@Immutable
data class SrDetailUiState(
    val profile: SrProfileUi? = null,
    val timeline: SrTimelineUi? = null,
    val work: SrWorkUi? = null,
    val target: SrTargetUi? = null,
    val isLoading: Boolean = false,
    val orderGroup: BreakdownGroup = BreakdownGroup.SkuWise,
    val orderQuery: String = "",
    val orders: ImmutableList<SrOrderUi> = persistentListOf(),
    val isOrdersLoading: Boolean = false,
    /** API failure popup; null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null
)

sealed interface SrDetailUiEffect {
    data object SessionExpired : SrDetailUiEffect
}
