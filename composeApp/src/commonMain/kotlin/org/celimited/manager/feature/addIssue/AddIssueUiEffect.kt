package org.celimited.manager.feature.addIssue

sealed interface AddIssueUiEffect {
    /** Ask for location permission; the answer comes back via onLocationPermissionResult. */
    data object RequestLocationPermission : AddIssueUiEffect
    /** The issue was saved and the result popup closed. */
    data object NavigateBack : AddIssueUiEffect
    data object SessionExpired : AddIssueUiEffect
}
