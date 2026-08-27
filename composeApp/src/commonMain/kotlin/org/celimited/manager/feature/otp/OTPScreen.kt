package org.celimited.manager.feature.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_app_logo
import org.celimited.manager.component.CornerRoundTextField
import org.celimited.manager.component.OtpInputField
import org.celimited.manager.component.PrimaryButton
import org.celimited.manager.feature.forgotPassword.ForgotPasswordScreen
import org.jetbrains.compose.resources.painterResource


@Composable
fun OTPRoute (
    onResetPassword:() -> Unit,
){
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->

        OTPScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onResetPassword
        )
    }
}

@Composable
fun OTPScreen (modifier: Modifier = Modifier, onResetPassword:() -> Unit) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.Center

    ) {


        Image(
            painter = painterResource(Res.drawable.ic_app_logo),
            contentDescription = "",
            modifier = Modifier.padding(bottom = 50.dp)
                .fillMaxWidth()
                .size(width = 170.dp, height = 40.dp),
            alignment = Alignment.Center
        )

        Text(
            text = "We will send you  an OTP to reset your password.",
            fontSize = 16.sp,
            modifier = Modifier
                .padding(bottom = 24.dp)
        )

        Text(
            text = "OTP verification",
            fontSize = 20.sp,
            color = Color.Black,
            modifier = Modifier
                .padding(bottom = 20.dp)
            )

        Text(
            text = "Enter the OTP send to 12345678909",
            fontSize = 14.sp,
            color = Color.Black
        )

        val modifier = Modifier
            .fillMaxWidth()

        var otp by rememberSaveable { mutableStateOf("") }

        OtpInputField(
            otpText = otp,
            otpCount = 4, // change to 4 if needed
            onOtpTextChange = { value, _ ->
                otp = value
            },
            modifier = modifier.padding(top = 16.dp)
        )

        Row (
            modifier = Modifier
                .padding(top = 20.dp)
        ){
            Text(
                text = "Didn’t received OTP?",
                fontSize = 14.sp,
                color = Color.Black
            )

            Text(
                text = "Resend",
                fontSize = 14.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 5.dp)
            )
        }

        PrimaryButton(
            text = "Verify",
            onClick = {
                onResetPassword()
            },
            containerColor = Color(0xFF582FFF),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
        )
    }
}