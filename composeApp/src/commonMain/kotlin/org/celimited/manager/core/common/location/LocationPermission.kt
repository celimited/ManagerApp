package org.celimited.manager.core.common.location

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that asks for foreground location permission and reports whether it was
 * granted. If permission is already granted, [onResult] is called with `true` without a prompt.
 */
@Composable
expect fun rememberLocationPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit
