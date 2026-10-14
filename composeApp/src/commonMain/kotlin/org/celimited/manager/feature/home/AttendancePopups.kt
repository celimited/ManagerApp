package org.celimited.manager.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_calendar
import manager.composeapp.generated.resources.ic_icon_clock
import manager.composeapp.generated.resources.ic_icon_info
import manager.composeapp.generated.resources.ic_icon_location
import org.celimited.manager.component.DropdownField
import org.celimited.manager.component.MessageDialog
import org.celimited.manager.component.PopupCard
import org.celimited.manager.component.PopupTitle
import org.celimited.manager.component.PrimaryButton
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val Purple = Color(0xFF582FFF)
private val PurpleBorder = Color(0xFF8B6CFF)
private val PurpleTint = Color(0xFFF3F0FF)
private val LateRed = Color(0xFFF0292F)
private val OnTimeGreen = Color(0xFF2BB24C)
private val LeaveOrange = Color(0xFFF5891F)
private val OrangeTint = Color(0xFFFFF4E8)
private val TextDark = Color(0xFF1E1E1E)
private val TextMuted = Color(0xFF6B7280)
private val FieldFill = Color(0xFFF7F7FB)

@Composable
fun AttendancePopupHost(
    popup: AttendancePopupUi,
    onDistributorSelected: (Int) -> Unit,
    onLateReasonSelected: (Int) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    onGeofenceWarningConfirmed: () -> Unit,
    onGeofenceWarningDismissed: () -> Unit
) {
    when (popup) {
        AttendancePopupUi.None -> Unit
        is AttendancePopupUi.CheckIn -> {
            CheckInPopup(
                isLate = popup.isLate,
                minutesRemaining = popup.minutesRemaining,
                form = popup.form,
                onDistributorSelected = onDistributorSelected,
                onLateReasonSelected = onLateReasonSelected,
                onRemarksChanged = onRemarksChanged,
                onSubmit = onSubmit,
                onSkip = onDismiss
            )
            popup.geofenceWarning?.let { warning ->
                GeofenceWarningDialog(
                    warning = warning,
                    onConfirm = onGeofenceWarningConfirmed,
                    onDismiss = onGeofenceWarningDismissed
                )
            }
        }
        is AttendancePopupUi.Leave -> LeavePopup(
            isApproved = popup.isApproved,
            fromDate = popup.fromDate,
            toDate = popup.toDate,
            onClose = onDismiss
        )
        AttendancePopupUi.WrongDeviceTime -> MessageDialog(
            title = "Wrong Device Time",
            titleColor = LateRed,
            message = "Your device time doesn't match the server time. Turn on automatic date & time " +
                "in your device settings, then log in again to mark attendance.",
            onClose = onDismiss
        )
        is AttendancePopupUi.Success -> MessageDialog(
            title = "Attendance Marked",
            titleColor = OnTimeGreen,
            message = popup.message,
            onClose = onDismiss
        )
    }
}

@Composable
private fun CheckInPopup(
    isLate: Boolean,
    minutesRemaining: Long?,
    form: AttendanceFormUi,
    onDistributorSelected: (Int) -> Unit,
    onLateReasonSelected: (Int) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit
) {
    PopupCard(onDismissRequest = onSkip) {
        PopupTitle(
            text = if (isLate) "You're Late !" else "Mark Attendance",
            color = if (isLate) LateRed else OnTimeGreen
        )

        if (isLate) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Attendance time has passed. Please provide a reason.",
                fontSize = 12.sp,
                color = TextMuted
            )
        } else if (minutesRemaining != null) {
            Spacer(Modifier.height(12.dp))
            Banner(icon = Res.drawable.ic_icon_clock, iconTint = Purple, background = PurpleTint) {
                Text(
                    text = buildAnnotatedString {
                        append("You have ")
                        withStyle(SpanStyle(color = Purple)) {
                            append("$minutesRemaining ${if (minutesRemaining == 1L) "minute" else "minutes"}")
                        }
                        append(" remaining to mark on-time attendance.")
                    },
                    fontSize = 12.sp,
                    color = TextDark
                )
            }
        }

        FieldLabel("Location")
        DropdownField(
            options = form.distributors,
            selectedId = form.selectedDistributorId,
            onSelected = onDistributorSelected,
            placeholder = "Select location",
            leadingIcon = Res.drawable.ic_icon_location,
            enabled = !form.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )

        if (isLate) {
            FieldLabel("Reason")
            DropdownField(
                options = form.lateReasons,
                selectedId = form.selectedReasonId,
                onSelected = onLateReasonSelected,
                placeholder = "Select reason",
                enabled = !form.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )

            FieldLabel("Remarks")
            OutlinedTextField(
                value = form.remarks,
                onValueChange = onRemarksChanged,
                enabled = !form.isSubmitting,
                placeholder = { Text("Remarks (optional)", fontSize = 13.sp) },
                textStyle = TextStyle(fontSize = 13.sp),
                minLines = 3,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleBorder,
                    unfocusedBorderColor = PurpleBorder,
                    focusedContainerColor = FieldFill,
                    unfocusedContainerColor = FieldFill
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        form.errorMessage?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(text = error, fontSize = 12.sp, color = LateRed)
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = when {
                form.isSubmitting -> "Submitting..."
                isLate -> "Submit Attendance"
                else -> "Give Attendance"
            },
            onClick = onSubmit,
            enabled = !form.isSubmitting,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Skip",
            color = Purple,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .clickable(enabled = !form.isSubmitting, onClick = onSkip)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun LeavePopup(
    isApproved: Boolean,
    fromDate: String,
    toDate: String,
    onClose: () -> Unit
) {
    PopupCard(onDismissRequest = onClose) {
        PopupTitle(text = if (isApproved) "On Leave" else "Leave Pending", color = LeaveOrange)

        Spacer(Modifier.height(12.dp))
        Banner(icon = Res.drawable.ic_icon_info, iconTint = LeaveOrange, background = OrangeTint) {
            Text(
                text = if (isApproved) "You are currently on approved leave." else "Your leave approval is pending.",
                fontSize = 12.sp,
                color = TextDark
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Res.drawable.ic_icon_calendar),
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isApproved) "Leave Period" else "Requested Leave Period",
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FieldFill, RoundedCornerShape(8.dp))
        ) {
            LeavePeriodRow(label = "From", value = fromDate)
            HorizontalDivider(color = Color(0xFFE5E7EB), modifier = Modifier.padding(horizontal = 12.dp))
            LeavePeriodRow(label = "To", value = toDate)
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Close", onClick = onClose, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun GeofenceWarningDialog(
    warning: GeofenceWarningUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = { Text("Outside Location Range", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                text = "You are ${warning.distanceMeters} m away from the selected location " +
                    "(allowed ${warning.radiusMeters} m). Do you still want to mark attendance?",
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Continue", color = Purple) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = TextDark,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun Banner(
    icon: DrawableResource,
    iconTint: Color,
    background: Color,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp).padding(top = 1.dp)
        )
        content()
    }
}

@Composable
private fun LeavePeriodRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextMuted)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }
}
