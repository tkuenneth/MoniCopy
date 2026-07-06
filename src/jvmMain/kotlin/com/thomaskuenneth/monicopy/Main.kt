package com.thomaskuenneth.monicopy

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thomaskuenneth.monicopy.appVersion
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.app_icon
import com.thomaskuenneth.monicopy.generated.resources.title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.thomaskuenneth.monicopy.app.AppViewModel
import com.thomaskuenneth.monicopy.app.DialogVisibility
import com.thomaskuenneth.monicopy.di.initKoin
import com.thomaskuenneth.monicopy.di.jvmModule
import com.thomaskuenneth.monicopy.ui.AboutWindow
import com.thomaskuenneth.monicopy.ui.MoniCopyApp
import com.thomaskuenneth.monicopy.ui.MoniCopyMenuBar
import com.thomaskuenneth.monicopy.ui.SettingsWindow
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
            title = "${stringResource(Res.string.title)} $appVersion",
            state = rememberWindowState(width = 720.dp, height = 480.dp),
            icon = painterResource(Res.drawable.app_icon),
        ) {
            MoniCopyApp(onClose = ::exitApplication) { viewModel, navigationState ->
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                with(Desktop.getDesktop()) {
                    LaunchedEffect(Unit) {
                        installPreferencesHandler { viewModel.setShouldShowSettings(true) }
                    }
                    LaunchedEffect(uiState.showExtendedAboutDialog) {
                        if (uiState.showExtendedAboutDialog) {
                            installAboutHandler { viewModel.setShouldShowAbout(true) }
                        } else {
                            installAboutHandler(null)
                        }
                    }
                }
                MoniCopyMenuBar(
                    navigationState = navigationState,
                    exit = ::exitApplication,
                    showAbout = { viewModel.setShouldShowAbout(true) },
                    showSettings = { viewModel.setShouldShowSettings(true) },
                )
                AboutWindow(
                    visible = uiState.aboutVisibility == DialogVisibility.Visible,
                    onCloseRequest = { viewModel.setShouldShowAbout(false) },
                )
                SettingsWindow(
                    visible = uiState.settingsVisibility == DialogVisibility.Visible,
                    viewModel = viewModel,
                    onCloseRequest = { viewModel.setShouldShowSettings(false) },
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
