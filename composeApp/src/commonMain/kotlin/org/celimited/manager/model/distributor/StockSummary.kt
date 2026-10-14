package org.celimited.manager.model.distributor

/** Stock tab data for one distributor. Money values are in taka. */
data class DistributorStockSummary(
    val pendingApproval: PendingApproval,
    val indentSuggestion: IndentSuggestion,
    val summary: StockSummaryCounts,
    val health: List<StockHealth>
)

data class PendingApproval(
    val indentCount: Int,
    val totalValue: Double
)

data class IndentSuggestion(
    val skuCount: Int,
    val belowCoverDays: Int,
    val estimatedValue: Double,
    val targetCoverDays: Int
)

data class StockSummaryCounts(
    val totalSkus: Int,
    val stockValue: Double,
    val stockValueChangePercent: Double,
    val skuBelowCoverCount: Int,
    val coverThresholdDays: Int,
    val zeroStockSkuCount: Int,
    val pendingIndentValue: Double,
    val pendingIndentCount: Int,
    val expiredSkuCount: Int,
    val nearlyExpiredSkuCount: Int,
    val nearlyExpiredWithinDays: Int
)

/** [level] is the API's raw bucket name, e.g. "CRITICAL". */
data class StockHealth(
    val level: String,
    val skuCount: Int
)
