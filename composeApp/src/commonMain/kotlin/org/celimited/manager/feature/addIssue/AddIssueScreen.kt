package org.celimited.manager.feature.addIssue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_calendar
import manager.composeapp.generated.resources.ic_icon_user
import org.celimited.manager.component.DropdownField
import org.celimited.manager.component.DropdownOption
import org.celimited.manager.component.ErrorDialog
import org.celimited.manager.component.MessageDialog
import org.celimited.manager.component.MinDatePickerDialog
import org.celimited.manager.component.PrimaryButton
import org.celimited.manager.component.TopBar
import org.celimited.manager.core.common.format.formatShortDate
import org.celimited.manager.core.common.location.rememberLocationPermissionRequester
import org.celimited.manager.model.distributor.IssueCategory
import org.celimited.manager.model.distributor.IssuePriority
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val Purple = Color(0xFF582FFF)
private val Orange = Color(0xFFFF6B00)
private val LabelColor = Color(0xFF1E2939)
private val TextColor = Color(0xFF1E1E1E)
private val PlaceholderColor = Color(0xFF9CA3AF)
private val FieldBorder = Color(0xFFE5E7EB)
private val ChipFill = Color(0xFFF3F4F6)
private val HintColor = Color(0xFF6B7280)
private val ErrorRed = Color(0xFFF0292F)
private val SuccessGreen = Color(0xFF2BB24C)

private val categoryOptions: ImmutableList<DropdownOption> =
    IssueCategory.entries.map { DropdownOption(it.ordinal, it.label) }.toImmutableList()

// Shown left to right, as in the design.
private val priorityOrder = listOf(IssuePriority.Low, IssuePriority.Medium, IssuePriority.High)

@Composable
fun AddIssueRoute(
    distributorId: Int,
    onBackClick: () -> Unit,
    onSessionExpired: () -> Unit,
    // ViewModels are activity-scoped here (no per-entry ViewModelStore), so key by id.
    viewModel: AddIssueViewModel = koinViewModel(
        key = "add-issue-$distributorId",
        parameters = { parametersOf(distributorId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestLocationPermission = rememberLocationPermissionRequester(
        onResult = viewModel::onLocationPermissionResult
    )

    // Survives rotation but not a new back-stack entry (see AISalesReportRoute), so the form is
    // cleared when the screen is genuinely re-entered.
    var didOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!didOpen) {
            didOpen = true
            viewModel.onScreenOpened()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                AddIssueUiEffect.RequestLocationPermission -> requestLocationPermission()
                AddIssueUiEffect.NavigateBack -> onBackClick()
                AddIssueUiEffect.SessionExpired -> onSessionExpired()
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopBar(
                title = "Add Issue",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        AddIssueScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            title = uiState.title,
            category = uiState.category,
            priority = uiState.priority,
            teamMembers = uiState.teamMembers,
            isLoadingTeamMembers = uiState.isLoadingTeamMembers,
            assigneeId = uiState.assigneeId,
            deadline = uiState.deadline,
            description = uiState.description,
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onTitleChanged = viewModel::onTitleChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onPrioritySelected = viewModel::onPrioritySelected,
            onAssigneeSelected = viewModel::onAssigneeSelected,
            onRetryTeamMembers = viewModel::onRetryTeamMembers,
            onDeadlineSelected = viewModel::onDeadlineSelected,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onSave = viewModel::onSave
        )
    }

    uiState.result?.let { result ->
        MessageDialog(
            title = if (result.isSuccess) "Issue Saved" else "Couldn't Save Issue",
            titleColor = if (result.isSuccess) SuccessGreen else ErrorRed,
            message = result.message,
            onClose = viewModel::onResultClosed
        )
    }

    uiState.errorDialog?.let { error ->
        ErrorDialog(
            message = error.message,
            onClose = viewModel::onDismissError,
            onRetry = if (error.canRetry) viewModel::onRetry else null
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun AddIssueScreen(
    modifier: Modifier,
    title: String,
    category: IssueCategory,
    priority: IssuePriority,
    teamMembers: ImmutableList<DropdownOption>,
    isLoadingTeamMembers: Boolean,
    assigneeId: Int?,
    deadline: LocalDate?,
    description: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onTitleChanged: (String) -> Unit,
    onCategorySelected: (IssueCategory) -> Unit,
    onPrioritySelected: (IssuePriority) -> Unit,
    onAssigneeSelected: (Int) -> Unit,
    onRetryTeamMembers: () -> Unit,
    onDeadlineSelected: (LocalDate) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
    ) {
        FieldLabel("Issue Title *")
        FormTextField(
            value = title,
            onValueChange = onTitleChanged,
            placeholder = "Brief description of the issue",
            enabled = !isSubmitting,
            singleLine = true
        )

        FieldLabel("Category")
        DropdownField(
            options = categoryOptions,
            selectedId = category.ordinal,
            onSelected = { onCategorySelected(IssueCategory.entries[it]) },
            placeholder = "Select category",
            enabled = !isSubmitting,
            borderColor = FieldBorder,
            modifier = Modifier.fillMaxWidth()
        )

        FieldLabel("Priority *")
        PrioritySelector(
            selected = priority,
            enabled = !isSubmitting,
            onSelected = onPrioritySelected
        )

        FieldLabel("Assign To *")
        DropdownField(
            options = teamMembers,
            selectedId = assigneeId,
            onSelected = onAssigneeSelected,
            placeholder = if (isLoadingTeamMembers) "Loading team members..." else "Select team member",
            leadingIcon = Res.drawable.ic_icon_user,
            enabled = !isSubmitting && !isLoadingTeamMembers,
            borderColor = FieldBorder,
            iconTint = Purple,
            modifier = Modifier.fillMaxWidth()
        )
        if (!isLoadingTeamMembers && teamMembers.isEmpty()) {
            Text(
                text = "Reload team members",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Purple,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable(enabled = !isSubmitting, onClick = onRetryTeamMembers)
            )
        }

        FieldLabel("Deadline *")
        DeadlineField(
            date = deadline,
            enabled = !isSubmitting,
            onClick = { showDatePicker = true }
        )

        FieldLabel("Description *")
        FormTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            placeholder = "Provide detailed information about the issue",
            enabled = !isSubmitting,
            singleLine = false,
            modifier = Modifier.heightIn(min = 120.dp)
        )
        Text(
            text = "${description.length} characters",
            fontSize = 11.sp,
            color = HintColor,
            modifier = Modifier.padding(top = 8.dp)
        )

        errorMessage?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(text = error, fontSize = 12.sp, color = ErrorRed)
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = if (isSubmitting) "Saving..." else "Save Issue",
            onClick = onSave,
            enabled = !isSubmitting,
            cornerRadius = 10.dp,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDatePicker) {
        MinDatePickerDialog(
            initial = deadline,
            minDate = today,
            onDateSelected = onDeadlineSelected,
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = LabelColor,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    singleLine: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4,
        placeholder = { Text(placeholder, fontSize = 13.sp, color = PlaceholderColor) },
        textStyle = TextStyle(fontSize = 13.sp, color = TextColor),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Purple,
            unfocusedBorderColor = FieldBorder,
            disabledBorderColor = FieldBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            disabledTextColor = TextColor
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PrioritySelector(
    selected: IssuePriority,
    enabled: Boolean,
    onSelected: (IssuePriority) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        priorityOrder.forEach { priority ->
            val isSelected = priority == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Orange else ChipFill)
                    .clickable(enabled = enabled) { onSelected(priority) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = priority.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextColor
                )
            }
        }
    }
}

@Composable
private fun DeadlineField(date: LocalDate?, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .border(1.dp, FieldBorder, shape)
            .background(Color.White)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_icon_calendar),
            contentDescription = null,
            tint = Purple,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = date?.let(::formatShortDate) ?: "Select deadline",
            fontSize = 13.sp,
            color = if (date == null) PlaceholderColor else TextColor
        )
    }
}
