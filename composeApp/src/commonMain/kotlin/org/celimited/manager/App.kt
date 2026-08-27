package org.celimited.manager

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.celimited.manager.feature.forgotPassword.ForgotPasswordRoute
import org.celimited.manager.feature.login.LoginRoute
import org.celimited.manager.feature.resetPassword.ResetPasswordRoute
import org.celimited.manager.navigation.AppNavigation

@Composable
@Preview
fun App() {
    MaterialTheme {
        AppNavigation()
    }
}