package com.thomaskuenneth.monicopy

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thomaskuenneth.monicopy.app.AppViewModel
import com.thomaskuenneth.monicopy.di.initKoin
import com.thomaskuenneth.monicopy.di.jvmModule
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.app_icon
import com.thomaskuenneth.monicopy.generated.resources.title
import com.thomaskuenneth.monicopy.ui.MoniCopyApp
import com.thomaskuenneth.monicopy.ui.MoniCopyMenuBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

fun main() {
    setupLogging()
    setupDockIcon()
    application {
        initKoin(jvmModule) {}
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(width = 720.dp, height = 480.dp),
            icon = painterResource(Res.drawable.app_icon),
        ) {
            val appViewModel: AppViewModel = koinViewModel()
            val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
            val title = stringResource(Res.string.title)
            LaunchedEffect(uiState.appVersion) {
                window.title = "$title ${uiState.appVersion}"
            }
            MoniCopyApp(appViewModel = appViewModel) { viewModel, navigationState ->
                with(Desktop.getDesktop()) {
                    LaunchedEffect(Unit) {
                        installPreferencesHandler { viewModel.showSettingsSheet(true) }
                    }
                    LaunchedEffect(uiState.showExtendedAboutDialog) {
                        if (uiState.showExtendedAboutDialog) {
                            installAboutHandler { viewModel.showAboutSheet(true) }
                        } else {
                            installAboutHandler(null)
                        }
                    }
                }
                MoniCopyMenuBar(
                    operatingSystem = uiState.operatingSystem,
                    navigationState = navigationState,
                    exit = ::exitApplication,
                    showAbout = { viewModel.showAboutSheet(true) },
                    showSettings = { viewModel.showSettingsSheet(true) },
                )
            }
        }
    }
}

private fun setupLogging() {
    val logger = Logger.getGlobal()
    try {
        val logFile = File(System.getProperty("user.home", "."), "MoniCopy.log")
        val handler = FileHandler(logFile.absolutePath, false)
        handler.formatter = SimpleFormatter()
        logger.addHandler(handler)
    } catch (e: IOException) {
        logger.log(java.util.logging.Level.SEVERE, "Could not create file handler", e)
    }
}
