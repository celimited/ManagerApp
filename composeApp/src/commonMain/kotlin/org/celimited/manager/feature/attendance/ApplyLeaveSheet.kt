package org.celimited.manager.feature.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_calendar
import org.celimited.manager.component.DropdownField
import org.celimited.manager.component.MinDatePickerDialog
import org.celimited.manager.component.PrimaryButton
import org.celimited.manager.core.common.format.formatShortDate
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val TitleNavy = Color(0xFF1F1059)
private val Purple = Color(0xFF582FFF)
private val FieldBorder = Color(0xFFCBBFFF)
private val FieldFill = Color(0xFFF7F7FB)
private val LabelColor = Color(0xFF1E1E1E)
private val PlaceholderColor = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFF0292F)

private enum class DateField { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ApplyLeaveSheet(
    form: ApplyLeaveFormUi,
    onFromDateSelected: (LocalDate) -> Unit,
    onToDateSelected: (LocalDate) -> Unit,
    onReasonSelected: (Int) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    // Swipe/tap-outside can't close the sheet mid-request.
    val isSubmitting by rememberUpdatedState(form.isSubmitting)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !isSubmitting }
    )
    var openPicker by remember { mutableStateOf<DateField?>(null) }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Text(
                text = "Apply Leave",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TitleNavy
            )
            HorizontalDivider(
                color = FieldBorder,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )

            FieldLabel("From Date")
            DateFieldBox(
                date = form.fromDate,
                enabled = !form.isSubmitting,
                onClick = { openPicker = DateField.FROM }
            )

            FieldLabel("To Date")
            DateFieldBox(
                date = form.toDate,
                enabled = !form.isSubmitting,
                onClick = { openPicker = DateField.TO }
            )

            FieldLabel("Reason")
            DropdownField(
                options = form.reasons,
                selectedId = form.reasonId,
                onSelected = onReasonSelected,
                placeholder = if (form.isLoadingReasons) "Loading reasons..." else "Choose your leave reason",
                enabled = !form.isSubmitting && !form.isLoadingReasons,
                borderColor = FieldBorder,
                modifier = Modifier.fillMaxWidth()
            )

            FieldLabel("Remarks")
            OutlinedTextField(
                value = form.remarks,
                onValueChange = onRemarksChanged,
                enabled = !form.isSubmitting,
                placeholder = { Text("Brief reason of your leave", fontSize = 13.sp, color = PlaceholderColor) },
                textStyle = TextStyle(fontSize = 13.sp),
                minLines = 3,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = FieldFill,
                    unfocusedContainerColor = FieldFill
                ),
                modifier = Modifier.fillMaxWidth()
            )

            form.errorMessage?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(text = error, fontSize = 12.sp, color = ErrorRed)
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = if (form.isSubmitting) "Submitting..." else "Apply Leave",
                onClick = onSubmit,
                enabled = !form.isSubmitting,
                cornerRadius = 28.dp,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    when (openPicker) {
        DateField.FROM -> MinDatePickerDialog(
            initial = form.fromDate,
            minDate = today,
            onDateSelected = onFromDateSelected,
            onDismiss = { openPicker = null }
        )
        DateField.TO -> MinDatePickerDialog(
            initial = form.toDate,
            minDate = form.fromDate?.takeIf { it > today } ?: today,
            onDateSelected = onToDateSelected,
            onDismiss = { openPicker = null }
        )
        null -> Unit
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = LabelColor,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun DateFieldBox(date: LocalDate?, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, FieldBorder, RoundedCornerShape(10.dp))
            .background(FieldFill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date?.let(::formatShortDate) ?: "Select date",
            fontSize = 13.sp,
            color = if (date == null) PlaceholderColor else LabelColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(Res.drawable.ic_icon_calendar),
            contentDescription = null,
            tint = Purple,
            modifier = Modifier.size(18.dp)
        )
    }
}
