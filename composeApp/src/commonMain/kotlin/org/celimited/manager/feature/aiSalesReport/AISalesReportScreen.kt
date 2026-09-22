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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.writeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_clock
import manager.composeapp.generated.resources.ic_icon_keyboard_next
import org.celimited.manager.component.TopBar
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AISalesReportRoute(
    onBackClick: () -> Unit,
    onSessionExpired: () -> Unit = onBackClick,
    viewModel: AISalesReportViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    val csvSaverLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onResult = { file ->
            val content = pendingCsvContent
            if (file != null && content != null) {
                scope.launch { file.writeString(content) }
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is AISalesReportUiEffect.SessionExpired -> onSessionExpired()
                is AISalesReportUiEffect.DownloadCsv -> {
                    pendingCsvContent = effect.csvContent
                    csvSaverLauncher.launch(
                        suggestedName = effect.suggestedName,
                        defaultExtension = "csv"
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
                title = "AI Report",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = viewModel::onHistoryClicked) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_icon_clock),
                            contentDescription = "Previous prompts",
                            modifier = Modifier.size(28.dp)
                        )
                    }
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
            onMetricSelected = viewModel::onMetricSelected
        )
    }

    if (uiState.isHistorySheetVisible) {
        PreviousPromptsSheet(
            isLoading = uiState.isHistoryLoading,
            prompts = uiState.previousPrompts,
            errorMessage = uiState.historyErrorMessage,
            onDismiss = viewModel::onDismissHistorySheet,
            onRetry = viewModel::onRetryPreviousPrompts,
            onPromptSelected = viewModel::onPreviousPromptSelected
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
    onMetricSelected: (Int) -> Unit
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

        when {
            uiState.isLoading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> item {
                ReportErrorCard(message = uiState.errorMessage, onRetry = onSubmitClicked)
            }

            uiState.result != null -> {
                if (uiState.result.rows.isNotEmpty()) {
                    item {
                        OutlinedButton(
                            onClick = onDownloadClicked,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text("Download CSV")
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
    Column {
        Text(
            text = "Ask a question about your data",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.promptInput,
            onValueChange = onPromptChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Total sales for salespoint A J Enterprise this month") },
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(8.dp)
        )

        uiState.usageRemaining?.let { remaining ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$remaining message${if (remaining == 1) "" else "s"} remaining today",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
        }

        Button(
            onClick = onSubmitClicked,
            enabled = !uiState.isLoading &&
                uiState.promptInput.isNotBlank() &&
                (uiState.usageRemaining == null || uiState.usageRemaining > 0),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Submit")
        }

        Spacer(Modifier.height(24.dp))
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

@Composable
private fun ReportErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE9E9), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(text = message, color = Color(0xFFB00020))
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text("Try again")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviousPromptsSheet(
    isLoading: Boolean,
    prompts: ImmutableList<QueryLogUi>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
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

                errorMessage != null -> ReportErrorCard(message = errorMessage, onRetry = onRetry)

                prompts.isEmpty() -> Text(
                    text = "No previous prompts yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(prompts, key = { it.queryLogId }) { item ->
                        PreviousPromptItem(item = item, onClick = { onPromptSelected(item) })
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
            .border(width = 1.dp, color = Color(0xFFCBBFFF), shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.userPrompt,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.executedAt,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
        }
        Icon(
            painter = painterResource(Res.drawable.ic_icon_keyboard_next),
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(20.dp)
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
