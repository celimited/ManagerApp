package org.celimited.manager.feature.aiSalesReport

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.write
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_clock
import manager.composeapp.generated.resources.ic_icon_mic
import manager.composeapp.generated.resources.ic_icon_send
import org.celimited.manager.component.ErrorDialog
import org.celimited.manager.component.PrimaryButton
import org.celimited.manager.component.TopBar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AISalesReportRoute(
    onBackClick: () -> Unit,
    onSessionExpired: () -> Unit = onBackClick,
    viewModel: AISalesReportViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Survives rotation but not a new back-stack entry: rememberSaveable state is held by
    // NavDisplay's SaveableStateHolder decorator, which drops it when the entry is popped. So the
    // screen resets when it is genuinely re-entered, but not when the Activity is recreated.
    var didOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!didOpen) {
            didOpen = true
            viewModel.onScreenOpened()
        }
    }

    val scope = rememberCoroutineScope()
    // One launcher serves both formats — the extension is a per-launch argument, not a per-launcher one.
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    val fileSaverLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onResult = { file ->
            val bytes = pendingBytes
            if (file != null && bytes != null) {
                scope.launch { file.write(bytes) }
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is AISalesReportUiEffect.SessionExpired -> onSessionExpired()
                is AISalesReportUiEffect.DownloadFile -> {
                    pendingBytes = effect.bytes
                    fileSaverLauncher.launch(
                        suggestedName = effect.suggestedName,
                        defaultExtension = effect.extension
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopBar(
                title = "AI Business Assistant",
                onBackClick = onBackClick,
                actions = {
                    CircleIconButton(
                        icon = Res.drawable.ic_icon_clock,
                        contentDescription = "Previous prompts",
                        background = Color.White,
                        tint = Color(0xFF475569),
                        enabled = true,
                        onClick = viewModel::onHistoryClicked
                    )
                }
            )
        }
    ) { padding ->
        AISalesReportScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            uiState = uiState,
            onPromptChanged = viewModel::onPromptChanged,
            onSubmitClicked = viewModel::onSubmitClicked,
            onDownloadClicked = viewModel::onDownloadClicked,
            onViewFormatSelected = viewModel::onViewFormatSelected,
            onLabelColumnSelected = viewModel::onLabelColumnSelected,
            onMetricSelected = viewModel::onMetricSelected,
            onFaqClicked = viewModel::onFaqSelected,
            onSummaryClicked = viewModel::onSummaryClicked
        )
    }

    if (uiState.isSummarySheetVisible) {
        SummarySheet(
            isLoading = uiState.isSummaryLoading,
            summary = uiState.summary,
            onDismiss = viewModel::onDismissSummarySheet
        )
    }

    if (uiState.isDownloadSheetVisible) {
        DownloadFormatSheet(
            selected = uiState.downloadFormat,
            onFormatSelected = viewModel::onDownloadFormatSelected,
            onConfirm = viewModel::onConfirmDownload,
            onDismiss = viewModel::onDismissDownloadSheet
        )
    }

    if (uiState.isHistorySheetVisible) {
        PreviousPromptsSheet(
            isLoading = uiState.isHistoryLoading,
            prompts = uiState.previousPrompts,
            onDismiss = viewModel::onDismissHistorySheet,
            onPromptSelected = viewModel::onPreviousPromptSelected
        )
    }

    // Every API failure (query, summary, history) is a dialog rather than an inline card so the
    // screen underneath stays unchanged.
    uiState.errorDialog?.let { error ->
        ErrorDialog(
            message = error.message,
            onClose = viewModel::onDismissError,
            onRetry = if (error.canRetry) viewModel::onRetryClicked else null
        )
    }
}

@Composable
fun AISalesReportScreen(
    modifier: Modifier = Modifier,
    uiState: AISalesReportUiState,
    onPromptChanged: (String) -> Unit,
    onSubmitClicked: () -> Unit,
    onDownloadClicked: () -> Unit,
    onViewFormatSelected: (ResultViewFormat) -> Unit,
    onLabelColumnSelected: (Int) -> Unit,
    onMetricSelected: (Int) -> Unit,
    onFaqClicked: (QueryLogUi) -> Unit = {},
    onSummaryClicked: () -> Unit = {}
) {
    // Shared across the header row and every data row so they scroll horizontally in lockstep.
    val horizontalScrollState = rememberScrollState()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            PromptSection(
                uiState = uiState,
                onPromptChanged = onPromptChanged,
                onSubmitClicked = onSubmitClicked
            )
        }

        if (uiState.result == null && !uiState.isLoading && uiState.faqs.isNotEmpty()) {
            item {
                FaqSection(faqs = uiState.faqs, onFaqClicked = onFaqClicked)
            }
        }

        when {
            uiState.isLoading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.result != null -> {
                if (uiState.result.rows.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = onDownloadClicked) {
                                Text("Download")
                            }
                            OutlinedButton(onClick = onSummaryClicked) {
                                Text("Summary")
                            }
                        }

                        ViewFormatSelector(
                            chartData = uiState.chartData,
                            selected = uiState.viewFormat,
                            onSelected = onViewFormatSelected
                        )

                        if (uiState.viewFormat != ResultViewFormat.Table && uiState.chartData != null) {
                            ChartColumnSelectors(
                                chartData = uiState.chartData,
                                selectedLabelColumnIndex = uiState.selectedLabelColumnIndex,
                                selectedMetricIndex = uiState.selectedMetricIndex,
                                onLabelColumnSelected = onLabelColumnSelected,
                                onMetricSelected = onMetricSelected
                            )
                        }
                    }
                }

                val chartData = uiState.chartData
                if (uiState.viewFormat == ResultViewFormat.Table || chartData == null) {
                    reportResultItems(
                        result = uiState.result,
                        horizontalScrollState = horizontalScrollState
                    )
                } else {
                    item {
                        ReportChart(
                            format = uiState.viewFormat,
                            chartData = chartData,
                            labelColumnIndex = uiState.selectedLabelColumnIndex,
                            metricIndex = uiState.selectedMetricIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptSection(
    uiState: AISalesReportUiState,
    onPromptChanged: (String) -> Unit,
    onSubmitClicked: () -> Unit
) {
    val canSubmit = !uiState.isLoading &&
        uiState.promptInput.isNotBlank() &&
        (uiState.usageRemaining == null || uiState.usageRemaining > 0)

    Column {
        Text(
            text = "Ask anything about your business data",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280)
        )

        Spacer(Modifier.height(12.dp))

        PromptInputCard(
            prompt = uiState.promptInput,
            canSubmit = canSubmit,
            onPromptChanged = onPromptChanged,
            onSubmitClicked = onSubmitClicked
        )

        uiState.usageRemaining?.let { remaining ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$remaining message${if (remaining == 1) "" else "s"} remaining today",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Prompt box: the input and the mic/send row are siblings in a [Column], so typed text never
 * reaches the button row and tapping that row is never a tap on the field.
 *
 * The field is deliberately left without a height modifier -- minLines/maxLines alone make it one
 * line at rest, grow it to two lines on wrap, and scroll it internally beyond that.
 */
@Composable
private fun PromptInputCard(
    prompt: String,
    canSubmit: Boolean,
    onPromptChanged: (String) -> Unit,
    onSubmitClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = prompt,
            onValueChange = onPromptChanged,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF111827)),
            cursorBrush = SolidColor(Color(0xFF582FFF)),
            minLines = 1,
            maxLines = 2,
            decorationBox = { innerTextField ->
                if (prompt.isEmpty()) {
                    Text(
                        text = "Ask your business question...",
                        fontSize = 16.sp,
                        color = Color(0xFF9CA3AF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(
                icon = Res.drawable.ic_icon_mic,
                contentDescription = "Voice input (coming soon)",
                background = Color(0xFFF3F4F6),
                tint = Color(0xFF9CA3AF),
                enabled = false,
                onClick = {}
            )

            Spacer(Modifier.width(8.dp))

            CircleIconButton(
                icon = Res.drawable.ic_icon_send,
                contentDescription = "Submit question",
                background = if (canSubmit) Color(0xFF582FFF) else Color(0xFFC7BDFF),
                tint = Color.White,
                enabled = canSubmit,
                onClick = {
                    // Drop focus first so the caret stops blinking, then dismiss the keyboard.
                    // Nothing re-focuses the field afterwards, so it stays dismissed through the
                    // request until the user taps the input again.
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSubmitClicked()
                }
            )
        }
    }
}

/**
 * Quick-start shortcuts shown while the screen has no result. Rendered as a single lazy item
 * rather than one per row: the card background, corners and dividers are one visual unit and the
 * list is only a handful of rows long.
 */
@Composable
private fun FaqSection(
    faqs: ImmutableList<QueryLogUi>,
    onFaqClicked: (QueryLogUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "FAQ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            modifier = Modifier.padding(vertical = 14.dp)
        )

        faqs.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFaqClicked(item) }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF582FFF))
                )

                Spacer(Modifier.width(16.dp))

                Text(
                    text = item.userPrompt,
                    fontSize = 14.sp,
                    color = Color(0xFF374151)
                )
            }

            if (index != faqs.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F0F7))
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: DrawableResource,
    contentDescription: String,
    background: Color,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ViewFormatSelector(
    chartData: ChartDataUi?,
    selected: ResultViewFormat,
    onSelected: (ResultViewFormat) -> Unit
) {
    val formats = buildList {
        add(ResultViewFormat.Table)
        if (chartData != null) {
            add(ResultViewFormat.Bar)
            add(ResultViewFormat.Line)
            if (chartData.allowsPie) add(ResultViewFormat.Pie)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        formats.forEach { format ->
            FilterChip(
                selected = format == selected,
                onClick = { onSelected(format) },
                label = { Text(format.name) }
            )
        }
    }

    if (chartData == null) {
        Text(
            text = "This result has no numeric column to chart.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ChartColumnSelectors(
    chartData: ChartDataUi,
    selectedLabelColumnIndex: Int,
    selectedMetricIndex: Int,
    onLabelColumnSelected: (Int) -> Unit,
    onMetricSelected: (Int) -> Unit
) {
    if (chartData.labelColumns.size > 1) {
        ChipRow(
            title = "Group by",
            options = chartData.labelColumns.map { it.name },
            selectedIndex = selectedLabelColumnIndex,
            onSelected = onLabelColumnSelected
        )
    }

    if (chartData.metrics.size > 1) {
        ChipRow(
            title = "Metric",
            options = chartData.metrics.map { it.name },
            selectedIndex = selectedMetricIndex,
            onSelected = onMetricSelected
        )
    }
}

@Composable
private fun ChipRow(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFF6B7280)
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, option ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                label = { Text(option) }
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ReportChart(
    format: ResultViewFormat,
    chartData: ChartDataUi,
    labelColumnIndex: Int,
    metricIndex: Int
) {
    val labelColumn = chartData.labelColumns.getOrNull(labelColumnIndex)
        ?: chartData.labelColumns.first()
    val metric = chartData.metrics.getOrNull(metricIndex) ?: chartData.metrics.first()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = metric.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        when (format) {
            ResultViewFormat.Bar -> ReportBarChart(labelColumn.labels, metric.values)
            ResultViewFormat.Line -> ReportLineChart(labelColumn.labels, metric.values)
            ResultViewFormat.Pie -> ReportPieChart(labelColumn.labels, metric.values)
            ResultViewFormat.Table -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummarySheet(
    isLoading: Boolean,
    summary: ReportSummaryUi?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Summary Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF582FFF)
            )

            Spacer(Modifier.height(16.dp))

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF582FFF))
                }

                summary != null -> {
                    if (summary.summary.isNotBlank()) {
                        SummarySectionLabel("SUMMARY")
                        Text(
                            text = summary.summary,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = Color(0xFF374151),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    if (summary.highlights.isNotEmpty()) {
                        SummarySectionLabel("HIGHLIGHTS")
                        summary.highlights.forEach { highlight ->
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                // Nudged down so the dot sits on the first line's optical centre.
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF582FFF))
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = highlight,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF374151)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (summary.actions.isNotEmpty()) {
                        SummarySectionLabel("ACTIONS")
                        summary.actions.forEachIndexed { index, action ->
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                // Numbered rather than bulleted: these are steps to follow, and the
                                // badge keeps them visually distinct from the highlights above.
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEDE9FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF582FFF)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = action,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF374151),
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummarySectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = Color(0xFF582FFF),
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadFormatSheet(
    selected: DownloadFormat,
    onFormatSelected: (DownloadFormat) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Download analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF582FFF)
            )

            Spacer(Modifier.height(12.dp))

            DownloadFormat.entries.forEachIndexed { index, format ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFormatSelected(format) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = format == selected,
                        onClick = { onFormatSelected(format) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF582FFF))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = format.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF374151)
                    )
                }

                if (index != DownloadFormat.entries.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F0F7))
                }
            }

            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = "Download",
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviousPromptsSheet(
    isLoading: Boolean,
    prompts: ImmutableList<PromptGroupUi>,
    onDismiss: () -> Unit,
    onPromptSelected: (QueryLogUi) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Previous prompts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                prompts.isEmpty() -> Text(
                    text = "No previous prompts yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                else -> LazyColumn {
                    prompts.forEach { group ->
                        item(key = "header-${group.label}") {
                            Text(
                                text = group.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.padding(top = 16.dp, bottom = 2.dp)
                            )
                        }

                        itemsIndexed(group.items, key = { _, item -> item.queryLogId }) { index, item ->
                            PreviousPromptItem(item = item, onClick = { onPromptSelected(item) })
                            // No rule after the last row — the next day's heading separates them.
                            if (index != group.items.lastIndex) {
                                HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F0F7))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PreviousPromptItem(item: QueryLogUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        // Top, not centre: the relative time should sit on the prompt's first line rather than
        // drifting down when the prompt wraps.
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.userPrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF111827)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.executedAt,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF)
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = item.relativeTime,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            maxLines = 1
        )
    }
}

private fun LazyListScope.reportResultItems(
    result: ReportResultUi,
    horizontalScrollState: ScrollState
) {
if (result.rows.isEmpty()) {
        item {
            Text(
                text = "No results found for this query.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    item {
        Box(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row {
                result.columns.forEach { column ->
                    Text(
                        text = column,
                        modifier = Modifier.width(160.dp).padding(8.dp),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    items(result.rows.size, key = { it }) { index ->
        val row = result.rows[index]
        Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
            row.forEach { cell ->
                Text(
                    text = cell,
                    modifier = Modifier.width(160.dp).padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
private fun AISalesReportScreenPreview() {
    AISalesReportScreen(
        uiState = AISalesReportUiState(),
        onPromptChanged = {},
        onSubmitClicked = {},
        onDownloadClicked = {},
        onViewFormatSelected = {},
        onLabelColumnSelected = {},
        onMetricSelected = {}
    )
}
