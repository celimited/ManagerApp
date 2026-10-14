package org.celimited.manager.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

const val UnderDevelopmentMessage = "This feature is under development"

/** Opacity of a locked (not yet built) feature. */
const val LockedAlpha = 0.6f

/**
 * Dims [content] and swallows every tap on it (inner clickables included) when [locked], calling
 * [onLockedClick] instead. Renders [content] untouched otherwise.
 */
@Composable
fun LockedFeature(
    locked: Boolean,
    onLockedClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Clips the tap ripple to the content's shape. */
    shape: Shape = RectangleShape,
    content: @Composable () -> Unit
) {
    if (!locked) {
        Box(modifier) { content() }
        return
    }
    Box(modifier) {
        Box(Modifier.alpha(LockedAlpha)) { content() }
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .semantics { stateDescription = UnderDevelopmentMessage }
                .clickable(onClick = onLockedClick)
        )
    }
}

/** Replaces any visible snackbar so repeated taps don't queue up a stack of them. */
suspend fun SnackbarHostState.showUnderDevelopment() {
    currentSnackbarData?.dismiss()
    showSnackbar(UnderDevelopmentMessage)
}
