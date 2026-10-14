package org.celimited.manager.feature.distributorDetail

sealed interface DistributorDetailUiEffect {
    data object SessionExpired : DistributorDetailUiEffect
    data object ShowUnderDevelopment : DistributorDetailUiEffect
}
