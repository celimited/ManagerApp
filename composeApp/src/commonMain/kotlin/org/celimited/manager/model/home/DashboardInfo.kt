package org.celimited.manager.model.home

/** Home dashboard data; [mtdPerformance] is null when the API sends no MTD row. */
data class DashboardInfo(
    val todayOrder: TodayOrder,
    val mtdPerformance: MtdPerformance?
)

data class TodayOrder(
    val totalSr: Int = 0,
    val presentSr: Int = 0,
    val orderStarted: Int = 0,
    val idleSr: Int = 0,
    val callProductivity: Double = 0.0,
    val visitProductivity: Double = 0.0,
    val lpc: Double = 0.0
)

data class MtdPerformance(
    val mtdAchieved: Double = 0.0,
    val ytdAchieved: Double = 0.0,
    val tillDateTarget: Double = 0.0,
    val mtdGrowth: Double = 0.0,
    val ytdGrowth: Double = 0.0,
    val m2mGrowth: Double = 0.0,
    val mtdSalesValue: Double = 0.0,
    val ytdSalesValue: Double = 0.0,
    val strikeRate: Double = 0.0,
    val lpc: Double = 0.0,
    val orderVsDelivery: Double = 0.0,
    val coverage: Double = 0.0
)
