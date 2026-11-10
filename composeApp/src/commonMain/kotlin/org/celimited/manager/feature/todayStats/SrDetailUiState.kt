package org.celimited.manager.feature.todayStats

import androidx.compose.runtime.Immutable
import org.celimited.manager.component.ErrorDialogUi

@Immutable
data class SrProfileUi(
    val name: String,
    val designation: String,
    val srCode: String,
    val territory: String,
    val isOnline: Boolean,
    val phoneNo: String,
    val whatsAppUrl: String?
)

@Immutable
data class SrTimelineUi(
    val spent: String,
    val loggedInTime: String,
    /** First visit time, or "No" when the SR hasn't visited yet. */
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
    val lpc: String,
    val remaining: String,
    val strikeRate: String,
    val productiveRate: String
)

@Immutable
data class SrTargetUi(
    val todayTarget: String,
    val todayOrder: String,
    val todayAchievementPercent: Int,
    val tillDateTarget: String,
    val tillDateOrder: String,
    val tillDateAchievementPercent: Int
)

@Immutable
data class SrOutletStatusUi(
    val activeOutlets: String,
    val zeroSalesOutlets: String,
    val activeSkus: String,
    val zeroSalesSkus: String
)

@Immutable
data class SrDetailUiState(
    val profile: SrProfileUi? = null,
    val timeline: SrTimelineUi? = null,
    val work: SrWorkUi? = null,
    val target: SrTargetUi? = null,
    val outlets: SrOutletStatusUi? = null,
    val isLoading: Boolean = false,
    /** API failure popup; null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null
)

sealed interface SrDetailUiEffect {
    data object SessionExpired : SrDetailUiEffect

    /** Open a tel: / https: link (dialer, WhatsApp). */
    data class OpenUri(val uri: String) : SrDetailUiEffect
}
