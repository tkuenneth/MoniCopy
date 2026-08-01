/*
 * Copyright 2017 - 2026 Thomas Kuenneth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thomaskuenneth.monicopy

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.thomaskuenneth.monicopy.app.AppViewModel
import com.thomaskuenneth.monicopy.di.MoniCopyKoinApp
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.app_icon
import com.thomaskuenneth.monicopy.generated.resources.title
import com.thomaskuenneth.monicopy.ui.MoniCopyApp
import com.thomaskuenneth.monicopy.ui.MoniCopyMenuBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.plugin.module.dsl.startKoin
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

fun main() {
    setupLogging()
    startKoin<MoniCopyKoinApp>()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(
                width = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp,
                height = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND.dp,
            ),
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
