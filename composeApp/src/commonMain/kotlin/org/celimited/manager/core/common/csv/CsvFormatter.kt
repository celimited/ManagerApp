package org.celimited.manager.core.common.csv

fun toCsv(columns: List<String>, rows: List<List<String>>): String {
    val builder = StringBuilder()
    builder.append(columns.joinToString(",") { it.toCsvField() }).append("\r\n")
    rows.forEach { row ->
        builder.append(row.joinToString(",") { it.toCsvField() }).append("\r\n")
    }
    return builder.toString()
}

private fun String.toCsvField(): String {
    return if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${replace("\"", "\"\"")}\""
    } else {
        this
    }
}
