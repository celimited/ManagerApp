package org.celimited.manager.data.mapper.distributor

import kotlinx.datetime.LocalDate
import org.celimited.manager.core.common.device.DeviceInfo
import org.celimited.manager.data.mapper.login.toDto
import org.celimited.manager.data.remote.dto.distributor.DistributorOverviewDto
import org.celimited.manager.data.remote.dto.distributor.SaveIssueRequestDto
import org.celimited.manager.data.remote.dto.distributor.SkuRefDto
import org.celimited.manager.data.remote.dto.distributor.StockSummaryDto
import org.celimited.manager.data.remote.dto.distributor.TeamMemberDto
import org.celimited.manager.model.distributor.DistributorOverview
import org.celimited.manager.model.distributor.DistributorStockSummary
import org.celimited.manager.model.distributor.IndentSuggestion
import org.celimited.manager.model.distributor.OverviewGrowth
import org.celimited.manager.model.distributor.OverviewInvoice
import org.celimited.manager.model.distributor.OverviewStockStatus
import org.celimited.manager.model.distributor.OverviewTarget
import org.celimited.manager.model.distributor.PendingApproval
import org.celimited.manager.model.distributor.SkuRef
import org.celimited.manager.model.distributor.StockHealth
import org.celimited.manager.model.distributor.StockSummaryCounts
import org.celimited.manager.model.distributor.SaveIssueParams
import org.celimited.manager.model.distributor.TeamMember

fun StockSummaryDto.toDomain(): DistributorStockSummary = DistributorStockSummary(
    pendingApproval = PendingApproval(
        indentCount = pendingApproval?.indentCount ?: 0,
        totalValue = pendingApproval?.totalValue ?: 0.0
    ),
    indentSuggestion = IndentSuggestion(
        skuCount = indentSuggestion?.skuCount ?: 0,
        belowCoverDays = indentSuggestion?.belowCoverDays ?: 0,
        estimatedValue = indentSuggestion?.estimatedValue ?: 0.0,
        targetCoverDays = indentSuggestion?.targetCoverDays ?: 0
    ),
    summary = StockSummaryCounts(
        totalSkus = summary?.totalSkus ?: 0,
        stockValue = summary?.stockValue ?: 0.0,
        stockValueChangePercent = summary?.stockValueChangePercent ?: 0.0,
        skuBelowCoverCount = summary?.skuBelowCoverCount ?: 0,
        coverThresholdDays = summary?.coverThresholdDays ?: 0,
        zeroStockSkuCount = summary?.zeroStockSkuCount ?: 0,
        pendingIndentValue = summary?.pendingIndentValue ?: 0.0,
        pendingIndentCount = summary?.pendingIndentCount ?: 0,
        expiredSkuCount = summary?.expiredSkuCount ?: 0,
        nearlyExpiredSkuCount = summary?.nearlyExpiredSkuCount ?: 0,
        nearlyExpiredWithinDays = summary?.nearlyExpiredWithinDays ?: 0
    ),
    health = health.orEmpty().mapNotNull { dto ->
        val level = dto.level?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        StockHealth(level = level, skuCount = dto.skuCount ?: 0)
    }
)

fun DistributorOverviewDto.toDomain(): DistributorOverview = DistributorOverview(
    lastVisitDate = lastVisitDate.parseDate(),
    phoneNumber = phoneNumber.orEmpty(),
    growth = OverviewGrowth(
        growthPercent = growth?.growthPercent ?: 0.0,
        mtdAchievementPercent = growth?.mtdAchievementPercent ?: 0.0,
        ytdAchievementPercent = growth?.ytdAchievementPercent ?: 0.0
    ),
    target = OverviewTarget(
        targetValue = target?.targetValue ?: 0.0,
        achievedValue = target?.achievedValue ?: 0.0,
        achievementPercent = target?.achievementPercent ?: 0.0,
        weeklyChangePercent = target?.weeklyChangePercent ?: 0.0
    ),
    invoice = OverviewInvoice(
        invoiceReceived = invoice?.invoiceReceived ?: false,
        lastInvoiceDate = invoice?.lastInvoiceDate.parseDate(),
        paymentStatus = invoice?.paymentStatus.orEmpty()
    ),
    stockStatus = OverviewStockStatus(
        zeroStockSkus = stockStatus?.zeroStockSkus.toSkuRefs(),
        slowMovingSkus = stockStatus?.slowMovingSkus.toSkuRefs(),
        slowMovingThresholdDays = stockStatus?.slowMovingThresholdDays ?: 0,
        stockHealthIndex = stockStatus?.stockHealthIndex ?: 0.0,
        inStockPercent = stockStatus?.inStockPercent ?: 0.0,
        lowStockPercent = stockStatus?.lowStockPercent ?: 0.0
    )
)

/** SKUs with neither a name nor a code have nothing to show, so they're dropped. */
private fun List<SkuRefDto>?.toSkuRefs(): List<SkuRef> = orEmpty().mapNotNull { dto ->
    val name = dto.skuName?.trim().orEmpty()
    val code = dto.skuCode?.trim().orEmpty()
    if (name.isEmpty() && code.isEmpty()) return@mapNotNull null
    SkuRef(id = dto.skuId ?: 0, code = code, name = name)
}

/** "2026-05-12", tolerating a trailing time part. */
private fun String?.parseDate(): LocalDate? =
    this?.takeIf { it.length >= 10 }?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

/** Entries without an id can't be assigned, so they're dropped. */
fun List<TeamMemberDto>.toTeamMembers(): List<TeamMember> = mapNotNull { dto ->
    val userId = dto.userId ?: return@mapNotNull null
    TeamMember(userId = userId, name = dto.name.orEmpty(), designation = dto.designation.orEmpty())
}

fun SaveIssueParams.toRequestDto(deviceInfo: DeviceInfo): SaveIssueRequestDto = SaveIssueRequestDto(
    distributorId = distributorId,
    title = title.trim(),
    // The API enums are the upper-case constant names.
    category = category.name.uppercase(),
    priority = priority.name.uppercase(),
    assignToUserId = assignToUserId,
    deadline = deadline.toString(),
    description = description.trim(),
    latitude = latitude.toString(),
    longitude = longitude.toString(),
    deviceInfo = deviceInfo.toDto()
)
