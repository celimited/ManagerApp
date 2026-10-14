package org.celimited.manager.core.common

/**
 * Features that are visible in the UI but not built yet. Locked ones render dimmed and show an
 * "under development" snackbar on tap — flip [isAvailable] to `true` once a feature ships.
 */
enum class Feature(val isAvailable: Boolean) {
    // Bottom navigation
    Approvals(false),
    OrderStatus(false),
    RetailVisit(false),
    Menu(false),

    // Home
    PendingApprovals(false),
    WorkPlan(false),
    MtdPerformance(false),
    Notifications(false),

    // Distributor details
    DistributorIssues(false),
    DistributorNotes(false),
    AddIssue(false),
    AddNote(false),
    DistributorHistory(false);

    val isLocked: Boolean get() = !isAvailable
}
