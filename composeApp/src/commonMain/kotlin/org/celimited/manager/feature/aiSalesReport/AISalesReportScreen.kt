package org.celimited.manager.feature.aiSalesReport

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import org.celimited.manager.component.TopBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AISalesReportRoute(
    onBackClick: () -> Unit,
    onSessionExpired: () -> Unit = onBackClick,
    viewModel: AISalesReportViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is AISalesReportUiEffect.SessionExpired -> onSessionExpired()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopBar(
                title = "AI Sales Report",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        AISalesReportScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            uiState = uiState,
            onPromptChanged = viewModel::onPromptChanged,
            onSubmitClicked = viewModel::onSubmitClicked
        )
    }
}

@Composable
fun AISalesReportScreen(
    modifier: Modifier = Modifier,
    uiState: AISalesReportUiState,
    onPromptChanged: (String) -> Unit,
    onSubmitClicked: () -> Unit
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

            uiState.result != null -> reportResultItems(
                result = uiState.result,
                horizontalScrollState = horizontalScrollState
            )
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
            text = "Ask a question about your sales data",
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

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onSubmitClicked,
            enabled = !uiState.isLoading && uiState.promptInput.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Ask")
        }

        Spacer(Modifier.height(24.dp))
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
        onSubmitClicked = {}
    )
}
