package org.celimited.manager.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Only dates on or after [minDate] can be picked. The picker works in UTC-midnight millis. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun MinDatePickerDialog(
    initial: LocalDate?,
    minDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = Color(0xFF582FFF)
) {
    val minMillis = remember(minDate) { minDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.takeIf { it >= minDate }
            ?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        yearRange = minDate.year..(minDate.year + 1),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= minMillis
            override fun isSelectableYear(year: Int): Boolean = year >= minDate.year
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        )
                    }
                    onDismiss()
                }
            ) { Text("OK", color = accentColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = accentColor) }
        }
    ) {
        DatePicker(state = state)
    }
}
