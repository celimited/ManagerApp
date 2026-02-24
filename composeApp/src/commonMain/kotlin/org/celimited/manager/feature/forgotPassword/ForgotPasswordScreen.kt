package org.celimited.manager.feature.forgotPassword

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_app_logo
import org.celimited.manager.component.CornerRoundTextField
import org.celimited.manager.component.PrimaryButton
import org.jetbrains.compose.resources.painterResource

@Composable
@Preview
fun ForgotPasswordRoute() {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->

        ForgotPasswordScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
fun ForgotPasswordScreen(modifier: Modifier = Modifier) {

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
            text = "Please enter your login ID to reset your password.",
            fontSize = 16.sp,
            modifier = Modifier
                .padding(bottom = 24.dp)
        )

        Text(
            text = "Login ID",
            fontSize = 14.sp,
            color = Color.Black
        )

        val modifier = Modifier
            .fillMaxWidth()

        var loginId by rememberSaveable { mutableStateOf("") }

        CornerRoundTextField(
            value = loginId,
            onValueChange = { loginId = it },
            modifier = modifier,
            hint = "Enter your login ID",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            focusedBorderColor = Color(0xFFCBBFFF),
            unfocusedBorderColor = Color(0xFFCBBFFF),
        )

        PrimaryButton(
            text = "Next",
            onClick = {},
            containerColor = Color(0xFF582FFF),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
        )
    }
}