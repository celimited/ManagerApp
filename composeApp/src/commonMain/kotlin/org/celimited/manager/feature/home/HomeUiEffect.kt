package org.celimited.manager.feature.home

sealed interface HomeUiEffect {
    data object NavigateToAttendance : HomeUiEffect
    data object RequestLocationPermission : HomeUiEffect
    data object NavigateToTodayStats : HomeUiEffect
    data object NavigateToDistributorList : HomeUiEffect
    data object ShowUnderDevelopment : HomeUiEffect
}
