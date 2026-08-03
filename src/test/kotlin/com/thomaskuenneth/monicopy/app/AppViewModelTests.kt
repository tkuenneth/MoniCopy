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
package com.thomaskuenneth.monicopy.app

import com.thomaskuenneth.monicopy.platform.OperatingSystem
import com.thomaskuenneth.monicopy.platform.PlatformInfo
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val AppViewModelTests by testSuite {
    testFixture {
        AppViewModelHarness()
    } asParameterForEach {
        test("sheet visibility toggles independently") { harness ->
            harness.viewModel.showAboutSheet(true)
            assertEquals(SheetVisibility.Visible, harness.viewModel.uiState.value.aboutVisibility)

            harness.viewModel.showSettingsSheet(true)
            harness.viewModel.showOpenSourceLicenses(true)
            assertEquals(SheetVisibility.Visible, harness.viewModel.uiState.value.settingsVisibility)
            assertEquals(SheetVisibility.Visible, harness.viewModel.uiState.value.openSourceLicensesVisibility)

            harness.viewModel.showAboutSheet(false)
            harness.viewModel.showSettingsSheet(false)
            harness.viewModel.showOpenSourceLicenses(false)
            assertEquals(SheetVisibility.Hidden, harness.viewModel.uiState.value.aboutVisibility)
            assertEquals(SheetVisibility.Hidden, harness.viewModel.uiState.value.settingsVisibility)
            assertEquals(SheetVisibility.Hidden, harness.viewModel.uiState.value.openSourceLicensesVisibility)
        }

        test("color scheme mode updates state and repository") { harness ->
            harness.viewModel.setColorSchemeMode(ColorSchemeMode.Dark)

            assertEquals(ColorSchemeMode.Dark, harness.viewModel.uiState.value.colorSchemeMode)
            assertEquals(ColorSchemeMode.Dark, harness.repository.storedColorSchemeMode)
        }

        test("extended about flag updates state and repository") { harness ->
            assertFalse(harness.viewModel.uiState.value.showExtendedAboutDialog)

            harness.viewModel.setShowExtendedAboutDialog(true)

            assertTrue(harness.viewModel.uiState.value.showExtendedAboutDialog)
            assertTrue(harness.repository.storedShowExtendedAboutDialog)
        }
    }
}

private class AppViewModelHarness {
    val repository = RecordingAppRepository()
    val viewModel = AppViewModel(
        repository = repository,
        platformInfo = FixedPlatformInfo(),
    )
}

private class RecordingAppRepository : AppRepository {
    var storedColorSchemeMode: ColorSchemeMode = ColorSchemeMode.System
    var storedShowExtendedAboutDialog: Boolean = false

    override fun getColorSchemeMode(): ColorSchemeMode = storedColorSchemeMode
    override fun setColorSchemeMode(value: ColorSchemeMode) {
        storedColorSchemeMode = value
    }

    override fun getShowExtendedAboutDialog(): Boolean = storedShowExtendedAboutDialog
    override fun setShowExtendedAboutDialog(value: Boolean) {
        storedShowExtendedAboutDialog = value
    }
}

private class FixedPlatformInfo : PlatformInfo {
    override val platformName: String = "test"
    override val appVersion: String = "0.0.0"
    override val appBuildVersion: String = "0"
    override val operatingSystem: OperatingSystem = OperatingSystem.Unknown
    override val showExtendedAboutDialogCheckbox: Boolean = true
}
