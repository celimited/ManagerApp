package org.celimited.manager

import androidx.compose.ui.window.ComposeUIViewController
import org.celimited.manager.core.di.initKoin
import org.celimited.manager.core.di.iosPlatformModule
import platform.UIKit.UIViewController

private var koinStarted = false

fun MainViewController(): UIViewController {
    if (!koinStarted) {
        initKoin(listOf(iosPlatformModule()))
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}