package org.celimited.manager.feature.aiSalesReport

sealed interface AISalesReportUiEffect {
    data object SessionExpired : AISalesReportUiEffect
}
