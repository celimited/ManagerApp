package org.celimited.manager.feature.todayStats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_chevron_down
import manager.composeapp.generated.resources.ic_icon_search
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// Palette shared by the Today's Stats screens.
internal val TitleColor = Color(0xFF111827)
internal val SubtleText = Color(0xFF6B7280)
internal val Accent = Color(0xFF582FFF)
internal val AccentSoft = Color(0xFFF1EEFF)
internal val CardBorder = Color(0xFFE5E7EB)
internal val TileBg = Color(0xFFF8F8FC)
internal val Track = Color(0xFFE9E6F7)
internal val Green = Color(0xFF16A34A)
internal val Red = Color(0xFFE11D48)
internal val Amber = Color(0xFFD97706)
internal val Orange = Color(0xFFEA580C)
internal val Blue = Color(0xFF2F80ED)

/** Background / border / text colors for a [StatTile]. */
internal enum class Tone(val background: Color, val border: Color, val content: Color) {
    Neutral(TileBg, CardBorder, TitleColor),
    Highlight(AccentSoft, Color(0xFFDDD6FE), Accent),
    Positive(Color(0xFFECFDF3), Color(0xFFBBF7D0), Green),
    Negative(Color(0xFFFEECEF), Color(0xFFFECDD3), Red),
    Warning(Color(0xFFFEF6E0), Color(0xFFFDE68A), Amber),
    Caution(Color(0xFFFFF1E6), Color(0xFFFED7AA), Orange)
}

/** White rounded card with an icon tile, title, optional subtitle and an optional trailing slot. */
@Composable
internal fun StatsCard(
    icon: DrawableResource,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        CardHeader(icon, title, subtitle, trailing)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun CardHeader(
    icon: DrawableResource,
    title: String,
    subtitle: String?,
    trailing: (@Composable () -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleColor)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = SubtleText)
            }
        }
        trailing?.invoke()
    }
}

@Composable
internal fun IconTile(icon: DrawableResource, size: Dp = 34.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(AccentSoft),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(size / 2)
        )
    }
}

/** Collapsible variant of [StatsCard] used on the My Team tab. Starts expanded. */
@Composable
internal fun CollapsibleStatsCard(
    icon: DrawableResource,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon)
            Spacer(Modifier.width(10.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleColor, modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(Res.drawable.ic_icon_chevron_down),
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = SubtleText,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(chevronRotation)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp), content = content)
        }
    }
}

/** Value-over-label tile; [tone] colors the background, border and value. */
@Composable
internal fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Neutral,
    valueSuffix: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tone.background)
            .border(1.dp, tone.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = tone.content, maxLines = 1)
            if (valueSuffix != null) {
                Spacer(Modifier.width(2.dp))
                Text(valueSuffix, fontSize = 10.sp, color = tone.content, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = if (tone == Tone.Neutral) SubtleText else tone.content,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/** Equal-width, equal-height row of tiles. */
@Composable
internal fun TileRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

internal fun RowScope.tileModifier() = Modifier.weight(1f).fillMaxHeight()

/** Small rounded pill, e.g. "90% Present". */
@Composable
internal fun Badge(text: String, tone: Tone) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tone.background)
            .border(1.dp, tone.border, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tone.content)
    }
}

@Composable
internal fun PercentRing(
    percent: Int,
    color: Color,
    size: Dp,
    strokeWidth: Dp,
    fontSize: Int = 11,
    center: (@Composable () -> Unit)? = null
) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { percent.toProgress() },
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = strokeWidth,
            trackColor = Track,
            strokeCap = StrokeCap.Round
        )
        if (center != null) {
            center()
        } else {
            Text("$percent%", fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = TitleColor)
        }
    }
}

@Composable
internal fun ProgressBar(fraction: Float, color: Color, height: Dp = 6.dp, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50)),
        color = color,
        trackColor = Track,
        drawStopIndicator = {}
    )
}

/** Top-level My Summary / My Team / SR switcher. */
@Composable
internal fun TopTabs(selected: TodayStatsTab, onSelect: (TodayStatsTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFECEAF5))
            .padding(4.dp)
    ) {
        TodayStatsTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) TitleColor else SubtleText
                )
            }
        }
    }
}

/** Horizontally scrolling pill chips; used for breakdown groups and hierarchy levels. */
@Composable
internal fun <T> PillChips(
    options: ImmutableList<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val shape = if (outlined) RoundedCornerShape(10.dp) else RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) Accent else Color.White)
                    .border(1.dp, if (isSelected) Accent else if (outlined) SubtleText else CardBorder, shape)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label(option),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TitleColor
                )
            }
        }
    }
}

@Composable
internal fun SearchToggleButton(active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) TitleColor else Accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_icon_search),
            contentDescription = "Search",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

// Hint and typed text share one style so they sit on the same baseline.
private val SearchTextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = TitleColor)

@Composable
internal fun SearchField(query: String, placeholder: String, onQueryChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = SearchTextStyle,
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_icon_search),
                    contentDescription = null,
                    tint = SubtleText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(placeholder, style = SearchTextStyle.copy(color = SubtleText))
                    inner()
                }
            }
        }
    )
}

/** Project / Product / SKU row: name, then Memo / Memo % / Order-vs-target (UOM) / Value tiles. */
@Composable
internal fun BreakdownCard(item: BreakdownItemUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        TileRow {
            StatTile(item.memo, "Memo", tileModifier())
            StatTile(item.memoPercent, "Memo %", tileModifier())
            StatTile(item.orderVsTargetUom, "Target/Order UOM", tileModifier())
            StatTile(item.orderValue, "Target/Order (Value)", tileModifier())
        }
    }
}

@Composable
internal fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
    }
}

@Composable
internal fun EmptyText(message: String) {
    Text(
        text = message,
        fontSize = 13.sp,
        color = SubtleText,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    )
}

/** Colored legend label over a bold value, e.g. "Present / 27". */
@Composable
internal fun LabeledValue(label: String, value: String, valueColor: Color = TitleColor, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = SubtleText, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
    }
}
