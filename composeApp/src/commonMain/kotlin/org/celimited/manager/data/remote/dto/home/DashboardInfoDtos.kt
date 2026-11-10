package org.celimited.manager.data.remote.dto.home

import kotlinx.serialization.Serializable

/** `data` of home/dashboard-info. */
@Serializable
data class DashboardInfoDataDto(
    val toDayOrder: TodayOrderDto? = null,
    val mtdPerformance: MtdPerformanceDto? = null
)

@Serializable
data class TodayOrderDto(
    val totalSr: Int? = null,
    val presentSr: Int? = null,
    val orderStarted: Int? = null,
    val idleSr: Int? = null,
    val callProductivity: Double? = null,
    val visitProductivity: Double? = null,
    val lpc: Double? = null
)

@Serializable
data class MtdPerformanceDto(
    val mtdAchive: Double? = null,
    val ytdAchive: Double? = null,
    val tillDateTarget: Double? = null,
    val mtdGrowth: Double? = null,
    val ytdGrowth: Double? = null,
    val m2m: Double? = null,
    val mtdSalesValue: Double? = null,
    val ytdSalesValue: Double? = null,
    val strikeRate: Double? = null,
    val lpc: Double? = null,
    val orderVsDelivery: Double? = null,
    val coverage: Double? = null
)
