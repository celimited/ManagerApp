package org.celimited.manager.model.distributor

import kotlinx.datetime.LocalDate

/** Overview tab data for one distributor. Money values are in taka, percents are 0–100. */
data class DistributorOverview(
    val lastVisitDate: LocalDate?,
    val phoneNumber: String,
    val growth: OverviewGrowth,
    val target: OverviewTarget,
    val invoice: OverviewInvoice,
    val stockStatus: OverviewStockStatus
)

data class OverviewGrowth(
    val growthPercent: Double,
    val mtdAchievementPercent: Double,
    val ytdAchievementPercent: Double
)

data class OverviewTarget(
    val targetValue: Double,
    val achievedValue: Double,
    val achievementPercent: Double,
    val weeklyChangePercent: Double
)

/** [paymentStatus] is the API's raw value, e.g. "PENDING". */
data class OverviewInvoice(
    val invoiceReceived: Boolean,
    val lastInvoiceDate: LocalDate?,
    val paymentStatus: String
)

data class OverviewStockStatus(
    val zeroStockSkus: List<SkuRef>,
    val slowMovingSkus: List<SkuRef>,
    val slowMovingThresholdDays: Int,
    val stockHealthIndex: Double,
    val inStockPercent: Double,
    val lowStockPercent: Double
)

data class SkuRef(
    val id: Int,
    val code: String,
    val name: String
)
