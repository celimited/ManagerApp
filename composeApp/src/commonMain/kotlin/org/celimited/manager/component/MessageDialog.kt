package org.celimited.manager.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Title + message + Close button, e.g. for a success or failure result. */
@Composable
fun MessageDialog(
    title: String,
    titleColor: Color,
    message: String,
    onClose: () -> Unit
) {
    PopupCard(onDismissRequest = onClose) {
        PopupTitle(text = title, color = titleColor)
        Spacer(Modifier.height(12.dp))
        Text(text = message, fontSize = 13.sp, color = Color(0xFF1E1E1E))
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Close", onClick = onClose, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
    }
}

/** UiState entry for an [ErrorDialog]; null in the state means no dialog. */
@Immutable
data class ErrorDialogUi(
    val message: String,
    /** False when retrying makes no sense from the dialog (e.g. the user re-submits a form instead). */
    val canRetry: Boolean = true
)

/**
 * Popup for a failed API call. Shown over the screen instead of inline, so the screen's own layout
 * never changes because of an error. "Try again" only appears when [onRetry] is given.
 */
@Composable
fun ErrorDialog(
    message: String,
    onClose: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    PopupCard(onDismissRequest = onClose) {
        PopupTitle(text = "Something went wrong", color = Color(0xFFF0292F))
        Spacer(Modifier.height(12.dp))
        Text(text = message, fontSize = 13.sp, color = Color(0xFF1E1E1E))
        Spacer(Modifier.height(20.dp))
        if (onRetry != null) {
            PrimaryButton(text = "Try again", onClick = onRetry, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = "Close",
                onClick = onClose,
                fontSize = 14.sp,
                containerColor = Color(0xFFEEEAFF),
                contentColor = Color(0xFF582FFF),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            PrimaryButton(text = "Close", onClick = onClose, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** White rounded dialog card that isn't dismissed by tapping outside. */
@Composable
internal fun PopupCard(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun PopupTitle(text: String, color: Color) {
    Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = color)
}
