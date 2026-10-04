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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.thomaskuenneth.monicopy.app.AppViewModel
import com.thomaskuenneth.monicopy.app.WindowPreferences
import com.thomaskuenneth.monicopy.app.WindowRepository
import com.thomaskuenneth.monicopy.app.WindowStateCalculator
import com.thomaskuenneth.monicopy.di.MoniCopyKoinApp
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.app_icon
import com.thomaskuenneth.monicopy.generated.resources.title
import com.thomaskuenneth.monicopy.ui.MoniCopyApp
import com.thomaskuenneth.monicopy.ui.MoniCopyMenuBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.plugin.module.dsl.startKoin
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

fun main() {
    startKoin<MoniCopyKoinApp>()
    val screenDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    val configuration = screenDevice.defaultConfiguration
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
    val screenBounds = configuration.bounds
    val density = configuration.defaultTransform.scaleX
    val screenPosition = DpOffset(
        x = ((screenBounds.x + insets.left) / density).dp,
        y = ((screenBounds.y + insets.top) / density).dp,
    )
    val screenSize = DpSize(
        width = ((screenBounds.width - insets.left - insets.right) / density).dp,
        height = ((screenBounds.height - insets.top - insets.bottom) / density).dp,
    )
    application {
        val windowRepository: WindowRepository = koinInject()
        val resolved = WindowStateCalculator.resolve(
            saved = windowRepository.load(),
            defaultSize = DpSize(
                width = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp,
                height = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND.dp,
            ),
            screenPosition = screenPosition,
            screenSize = screenSize,
        )
        val windowState = rememberWindowState(
            placement = resolved.placement,
            isMinimized = resolved.isMinimized,
            position = resolved.position?.let { WindowPosition(it.x, it.y) } ?: WindowPosition.PlatformDefault,
            width = resolved.size.width,
            height = resolved.size.height,
        )
        var floatingBounds by remember {
            mutableStateOf(
                WindowPreferences(
                    x = resolved.position?.x?.value,
                    y = resolved.position?.y?.value,
                    width = resolved.size.width.value,
                    height = resolved.size.height.value,
                ),
            )
        }
        LaunchedEffect(windowState) {
            snapshotFlow { Triple(windowState.placement, windowState.position, windowState.size) }
                .collect { (placement, position, size) ->
                    floatingBounds = WindowStateCalculator.captureFloatingBounds(
                        placement = placement,
                        position = position,
                        size = size,
                        current = floatingBounds,
                    )
                }
        }
        fun saveWindowState() {
            windowRepository.save(
                floatingBounds.copy(
                    placement = windowState.placement.name,
                    isMinimized = windowState.isMinimized,
                ),
            )
        }
        Window(
            onCloseRequest = {
                saveWindowState()
                exitApplication()
            },
            state = windowState,
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
                    LaunchedEffect(Unit) {
                        installQuitHandler { _, response ->
                            saveWindowState()
                            response.performQuit()
                        }
                    }
                }
                MoniCopyMenuBar(
                    operatingSystem = uiState.operatingSystem,
                    navigationState = navigationState,
                    exit = {
                        saveWindowState()
                        exitApplication()
                    },
                    showAbout = { viewModel.showAboutSheet(true) },
                    showOpenSourceLicenses = { viewModel.showOpenSourceLicenses(true) },
                    showSettings = { viewModel.showSettingsSheet(true) },
                )
            }
        }
    }
}
