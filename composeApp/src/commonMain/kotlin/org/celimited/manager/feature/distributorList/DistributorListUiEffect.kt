package org.celimited.manager.feature.distributorList

sealed interface DistributorListUiEffect {
    data object SessionExpired : DistributorListUiEffect
    data class NavigateToDetail(val distributorId: Int) : DistributorListUiEffect
}
