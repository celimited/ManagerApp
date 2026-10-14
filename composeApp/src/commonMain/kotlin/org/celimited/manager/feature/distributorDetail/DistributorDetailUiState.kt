package org.celimited.manager.feature.distributorDetail

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.core.common.Feature
import org.celimited.manager.model.distributor.IssuePriority

/** [feature] null = always available. */
enum class DistributorDetailTab(val label: String, private val feature: Feature? = null) {
    Overview("Overview"),
    Issues("Issues", Feature.DistributorIssues),
    Stock("Stock"),
    Notes("Notes", Feature.DistributorNotes);

    val isLocked: Boolean get() = feature?.isLocked == true
}

@Immutable
data class DistributorOverviewUi(
    /** e.g. "+8.5%" */
    val growthPercent: String,
    val isGrowthNegative: Boolean,
    val mtdPercent: Int,
    val ytdPercent: Int,
    /** Can exceed 100; the ring clamps, the label doesn't. */
    val targetPercent: Int,
    val targetAchieved: String,
    val target: String,
    /** e.g. "+4.2%" */
    val weeklyChange: String,
    val isWeeklyChangeNegative: Boolean,
    val invoiceReceived: Boolean,
    val lastInvoiceDate: String,
    val paymentStatus: String,
    val isPaymentSettled: Boolean,
    val stock: DistributorStockUi
)

@Immutable
data class DistributorStockUi(
    val zeroStockSkus: ImmutableList<String>,
    val slowMovingSkus: ImmutableList<String>,
    val slowMovingThresholdDays: Int,
    val healthPercent: Int,
    val inStockPercent: Int,
    val lowPercent: Int
)

enum class IssueStatus(val label: String) {
    Pending("Pending"),
    InProgress("In Progress"),
    Resolved("Resolved")
}

@Immutable
data class DistributorIssueUi(
    val id: Int,
    val title: String,
    val priority: IssuePriority,
    val status: IssueStatus,
    val assignedTo: String,
    val dueDate: String
)

enum class StockSummaryTone { Positive, Warning, Danger, Info }

@Immutable
data class StockSummaryItemUi(
    val id: String,
    val label: String,
    val value: String,
    val note: String,
    val tone: StockSummaryTone
)

/** Declaration order is the bar's left-to-right order (worst first). */
enum class StockHealthLevel(val label: String) {
    Zero("Out of Stock"),
    Critical("Critical"),
    Watch("Low Stock"),
    Healthy("Healthy")
}

@Immutable
data class StockHealthBucketUi(
    val level: StockHealthLevel,
    val skuCount: Int
)

@Immutable
data class DistributorStockTabUi(
    val pendingApprovalCount: Int,
    val pendingApprovalValue: String,
    val suggestionText: String,
    val totalSkus: Int,
    val summary: ImmutableList<StockSummaryItemUi>,
    val health: ImmutableList<StockHealthBucketUi>
) {
    companion object {
        /** Before the stock-summary API responds; empty sections are hidden. */
        val Empty = DistributorStockTabUi(
            pendingApprovalCount = 0,
            pendingApprovalValue = "",
            suggestionText = "",
            totalSkus = 0,
            summary = persistentListOf(),
            health = persistentListOf()
        )
    }
}

@Immutable
data class DistributorDetailUiState(
    // From the cached distributor (name / code / address come from the API).
    val name: String = "",
    val code: String = "",
    val address: String = "",
    // From the overview API; blank hides the "Last visit" line.
    val lastVisit: String = "",
    /** Null until the overview API responds. */
    val overview: DistributorOverviewUi? = null,
    val isOverviewLoading: Boolean = false,
    /** Filled from the stock-summary API. */
    val stockTab: DistributorStockTabUi = DistributorStockTabUi.Empty,
    val isStockSummaryLoading: Boolean = false,
    /** Already filtered by [issueQuery]. */
    val issues: ImmutableList<DistributorIssueUi> = DistributorDetailDummyData.issues,
    val issueQuery: String = "",
    val selectedTab: DistributorDetailTab = DistributorDetailTab.Overview,
    /** API failure popup; null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null
)

internal object DistributorDetailDummyData {
    val issues: ImmutableList<DistributorIssueUi> = persistentListOf(
        DistributorIssueUi(1, "Payment delay - April invoice", IssuePriority.High, IssueStatus.InProgress, "Rahul Kumar", "25 May 2026"),
        DistributorIssueUi(2, "Refrigerator maintenance required", IssuePriority.Medium, IssueStatus.Pending, "Service Team", "20 May 2026"),
        DistributorIssueUi(3, "Stock discrepancy - SKU #4521", IssuePriority.Medium, IssueStatus.InProgress, "Inventory Team", "22 May 2026")
    )
}
