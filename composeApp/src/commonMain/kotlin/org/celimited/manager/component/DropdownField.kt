package org.celimited.manager.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_chevron_down
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Immutable
data class DropdownOption(
    val id: Int,
    val label: String
)

/** Rounded select field that opens a menu of [options] sized to the field's width. */
@Composable
fun DropdownField(
    options: ImmutableList<DropdownOption>,
    selectedId: Int?,
    onSelected: (Int) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: DrawableResource? = null,
    enabled: Boolean = true,
    borderColor: Color = Color(0xFF8B6CFF),
    iconTint: Color = Color(0xFF582FFF)
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidthPx by remember { mutableIntStateOf(0) }
    val selectedLabel = remember(options, selectedId) {
        options.firstOrNull { it.id == selectedId }?.label
    }
    val shape = RoundedCornerShape(10.dp)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .onSizeChanged { fieldWidthPx = it.width }
                .clip(shape)
                .background(Color.White)
                .border(1.dp, borderColor, shape)
                .clickable(enabled = enabled && options.isNotEmpty()) { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                // A disabled field is usually still loading — keep its placeholder (e.g. "Loading...").
                text = selectedLabel ?: if (options.isEmpty() && enabled) "No options available" else placeholder,
                color = if (selectedLabel != null) Color(0xFF1E1E1E) else Color(0xFF9CA3AF),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(Res.drawable.ic_icon_chevron_down),
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { fieldWidthPx.toDp() })
                .heightIn(max = 280.dp),
            containerColor = Color.White
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontSize = 13.sp,
                            color = if (option.id == selectedId) iconTint else Color(0xFF1E1E1E)
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(option.id)
                    }
                )
            }
        }
    }
}
