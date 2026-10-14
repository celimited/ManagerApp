package org.celimited.manager.feature.aiSalesReport

sealed interface AISalesReportUiEffect {
    data object SessionExpired : AISalesReportUiEffect

    /**
     * Plain class rather than a data class: [bytes] would give the generated `equals`/`hashCode`
     * array-identity semantics, which is misleading for a value-looking type.
     */
    class DownloadFile(
        val suggestedName: String,
        val extension: String,
        val bytes: ByteArray
    ) : AISalesReportUiEffect
}
