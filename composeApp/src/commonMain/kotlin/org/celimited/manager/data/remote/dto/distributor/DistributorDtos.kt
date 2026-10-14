package org.celimited.manager.data.remote.dto.distributor

import kotlinx.serialization.Serializable
import org.celimited.manager.data.remote.dto.login.DeviceInfoDto

@Serializable
data class TeamMemberDto(
    val userId: Int? = null,
    val name: String? = null,
    val designation: String? = null
)

@Serializable
data class StockSummaryDto(
    val distributorId: Int? = null,
    val pendingApproval: PendingApprovalDto? = null,
    val indentSuggestion: IndentSuggestionDto? = null,
    val summary: StockSummaryCountsDto? = null,
    val health: List<StockHealthDto>? = null
)

@Serializable
data class PendingApprovalDto(
    val indentCount: Int? = null,
    val totalValue: Double? = null
)

@Serializable
data class IndentSuggestionDto(
    val skuCount: Int? = null,
    val belowCoverDays: Int? = null,
    val estimatedValue: Double? = null,
    val targetCoverDays: Int? = null
)

@Serializable
data class StockSummaryCountsDto(
    val totalSkus: Int? = null,
    val stockValue: Double? = null,
    val stockValueChangePercent: Double? = null,
    val skuBelowCoverCount: Int? = null,
    val coverThresholdDays: Int? = null,
    val zeroStockSkuCount: Int? = null,
    val pendingIndentValue: Double? = null,
    val pendingIndentCount: Int? = null,
    val expiredSkuCount: Int? = null,
    val nearlyExpiredSkuCount: Int? = null,
    val nearlyExpiredWithinDays: Int? = null
)

@Serializable
data class StockHealthDto(
    val level: String? = null,
    val skuCount: Int? = null
)

@Serializable
data class DistributorOverviewDto(
    val distributorId: Int? = null,
    /** yyyy-MM-dd */
    val lastVisitDate: String? = null,
    val phoneNumber: String? = null,
    val growth: GrowthDto? = null,
    val target: TargetDto? = null,
    val invoice: InvoiceDto? = null,
    val stockStatus: OverviewStockStatusDto? = null
)

@Serializable
data class GrowthDto(
    val growthPercent: Double? = null,
    val mtdAchievementPercent: Double? = null,
    val ytdAchievementPercent: Double? = null
)

@Serializable
data class TargetDto(
    val targetValue: Double? = null,
    val achievedValue: Double? = null,
    val achievementPercent: Double? = null,
    val weeklyChangePercent: Double? = null
)

@Serializable
data class InvoiceDto(
    val invoiceReceived: Boolean? = null,
    /** yyyy-MM-dd */
    val lastInvoiceDate: String? = null,
    /** e.g. PENDING / PAID */
    val paymentStatus: String? = null
)

@Serializable
data class OverviewStockStatusDto(
    val zeroStockSkus: List<SkuRefDto>? = null,
    val slowMovingSkus: List<SkuRefDto>? = null,
    val slowMovingThresholdDays: Int? = null,
    val stockHealthIndex: Double? = null,
    val inStockPercent: Double? = null,
    val lowStockPercent: Double? = null
)

@Serializable
data class SkuRefDto(
    val skuId: Int? = null,
    val skuCode: String? = null,
    val skuName: String? = null
)

@Serializable
data class SaveIssueRequestDto(
    val distributorId: Int,
    val title: String,
    /** GENERAL / PAYMENT / STOCK / DELIVERY / EQUIPMENT / OTHER */
    val category: String,
    /** HIGH / MEDIUM / LOW */
    val priority: String,
    val assignToUserId: Int,
    /** yyyy-MM-dd */
    val deadline: String,
    val description: String,
    val latitude: String,
    val longitude: String,
    val deviceInfo: DeviceInfoDto
)
