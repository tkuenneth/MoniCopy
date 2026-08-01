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
package com.thomaskuenneth.monicopy.platform

import org.koin.core.annotation.Single
import java.util.ResourceBundle

@Single
class DefaultPlatformInfo : PlatformInfo {
    private val versionBundle = ResourceBundle.getBundle("version")

    override val platformName: String = buildString {
        append(System.getProperty("os.name") ?: "")
        append(' ')
        append(System.getProperty("os.version") ?: "")
        appendLine()
        append(System.getProperty("java.vendor") ?: "")
        append(' ')
        append(System.getProperty("java.vendor.version") ?: "")
        append(" (")
        append(System.getProperty("os.arch") ?: "")
        append(')')
    }

    override val appVersion: String = versionBundle.getString("VERSION")

    override val appBuildVersion: String = versionBundle.getString("BUILD_VERSION")

    override val operatingSystem: OperatingSystem = when {
        platformName.contains("mac os x", ignoreCase = true) -> OperatingSystem.MacOS
        platformName.contains("windows", ignoreCase = true) -> OperatingSystem.Windows
        platformName.contains("linux", ignoreCase = true) -> OperatingSystem.Linux
        else -> OperatingSystem.Unknown
    }

    override val showExtendedAboutDialogCheckbox: Boolean =
        operatingSystem == OperatingSystem.MacOS
}
