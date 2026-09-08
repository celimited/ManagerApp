package org.celimited.manager.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.celimited.manager.component.CornerRoundTextField
import org.celimited.manager.component.LoadingOverlay
import org.celimited.manager.component.PrimaryButton
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_app_logo
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun LoginRoute(
    onLogin: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is LoginUiEffect.NavigateToHome -> onLogin()
                is LoginUiEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LoadingOverlay(isVisible = uiState.isLoading)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        LoginScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            uiState = uiState,
            onLoginIdChanged = viewModel::onLoginIdChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onLoginClicked = viewModel::onLoginClicked,
            onForgotPasswordClick = onForgotPasswordClick
        )
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    uiState: LoginUiState,
    onLoginIdChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {

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

        val fieldModifier = Modifier
            .fillMaxWidth()

        Text(
            text = "Login ID",
            fontSize = 14.sp,
            color = Color.Black
        )

        CornerRoundTextField(
            value = uiState.loginId,
            onValueChange = onLoginIdChanged,
            modifier = fieldModifier,
            hint = "Enter your login ID",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            focusedBorderColor = Color(0xFFCBBFFF),
            unfocusedBorderColor = Color(0xFFCBBFFF),
        )

        Text(
            text = "Password",
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.padding(top = 24.dp)
        )

        CornerRoundTextField(
            value = uiState.password,
            onValueChange = onPasswordChanged,
            modifier = fieldModifier,
            hint = "Enter your password",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            focusedBorderColor = Color(0xFFCBBFFF),
            unfocusedBorderColor = Color(0xFFCBBFFF),
        )

        Text(
            text = "Forgot password?",
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .clickable(onClick = {
                    onForgotPasswordClick()
                }),
            textAlign = TextAlign.End
        )

        PrimaryButton(
            text = if (uiState.isLoading) "Logging in..." else "Login",
            onClick = onLoginClicked,
            enabled = !uiState.isLoading,
            containerColor = Color(0xFF582FFF),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
        )
    }
}

@Composable
@Preview (showBackground = true, showSystemUi = true)
private fun LoginScreenPreview() {
    LoginScreen(
        uiState = LoginUiState(),
        onLoginIdChanged = {},
        onPasswordChanged = {},
        onLoginClicked = {},
        onForgotPasswordClick = {}
    )
}
