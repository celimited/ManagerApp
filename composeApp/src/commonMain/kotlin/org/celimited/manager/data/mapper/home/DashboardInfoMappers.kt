package org.celimited.manager.data.mapper.home

import org.celimited.manager.data.remote.dto.home.DashboardInfoDataDto
import org.celimited.manager.data.remote.dto.home.MtdPerformanceDto
import org.celimited.manager.data.remote.dto.home.TodayOrderDto
import org.celimited.manager.model.home.DashboardInfo
import org.celimited.manager.model.home.MtdPerformance
import org.celimited.manager.model.home.TodayOrder

fun DashboardInfoDataDto.toDomain(): DashboardInfo = DashboardInfo(
    todayOrder = toDayOrder?.toDomain() ?: TodayOrder(),
    mtdPerformance = mtdPerformance?.toDomain()
)

fun TodayOrderDto.toDomain(): TodayOrder = TodayOrder(
    totalSr = totalSr ?: 0,
    presentSr = presentSr ?: 0,
    orderStarted = orderStarted ?: 0,
    idleSr = idleSr ?: 0,
    callProductivity = callProductivity ?: 0.0,
    visitProductivity = visitProductivity ?: 0.0,
    lpc = lpc ?: 0.0
)

fun MtdPerformanceDto.toDomain(): MtdPerformance = MtdPerformance(
    mtdAchieved = mtdAchive ?: 0.0,
    ytdAchieved = ytdAchive ?: 0.0,
    tillDateTarget = tillDateTarget ?: 0.0,
    mtdGrowth = mtdGrowth ?: 0.0,
    ytdGrowth = ytdGrowth ?: 0.0,
    m2mGrowth = m2m ?: 0.0,
    mtdSalesValue = mtdSalesValue ?: 0.0,
    ytdSalesValue = ytdSalesValue ?: 0.0,
    strikeRate = strikeRate ?: 0.0,
    lpc = lpc ?: 0.0,
    orderVsDelivery = orderVsDelivery ?: 0.0,
    coverage = coverage ?: 0.0
)
