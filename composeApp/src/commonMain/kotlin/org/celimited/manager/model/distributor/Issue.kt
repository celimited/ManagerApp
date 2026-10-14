package org.celimited.manager.model.distributor

import kotlinx.datetime.LocalDate

enum class IssuePriority(val label: String) {
    High("High"),
    Medium("Medium"),
    Low("Low")
}

enum class IssueCategory(val label: String) {
    General("General"),
    Payment("Payment"),
    Stock("Stock"),
    Delivery("Delivery"),
    Equipment("Equipment"),
    Other("Other")
}

/** Someone an issue can be assigned to. */
data class TeamMember(
    val userId: Int,
    val name: String,
    val designation: String
)

data class SaveIssueParams(
    val distributorId: Int,
    val title: String,
    val category: IssueCategory,
    val priority: IssuePriority,
    val assignToUserId: Int,
    val deadline: LocalDate,
    val description: String,
    /** Device location at the time of saving. */
    val latitude: Double,
    val longitude: Double
)
