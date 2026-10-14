package org.celimited.manager.feature.todayStats

sealed interface TodayStatsUiEffect {
    data class NavigateToSrDetail(val srId: Int) : TodayStatsUiEffect
    data object SessionExpired : TodayStatsUiEffect
}
