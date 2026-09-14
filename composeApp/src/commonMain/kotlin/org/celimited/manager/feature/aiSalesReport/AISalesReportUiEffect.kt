package org.celimited.manager.feature.aiSalesReport

sealed interface AISalesReportUiEffect {
    data object SessionExpired : AISalesReportUiEffect
    data class DownloadCsv(val suggestedName: String, val csvContent: String) : AISalesReportUiEffect
}
