package com.thomaskuenneth.monicopy.jvm.platform

import com.thomaskuenneth.monicopy.platform.OperatingSystem
import com.thomaskuenneth.monicopy.platform.PlatformInfo
import org.koin.core.annotation.Single
import java.util.ResourceBundle

@Single
class JvmPlatformInfo : PlatformInfo {
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

    override val appVersion: String = ResourceBundle.getBundle("version").getString("VERSION")

    override val operatingSystem: OperatingSystem = when {
        platformName.contains("mac os x", ignoreCase = true) -> OperatingSystem.MacOS
        platformName.contains("windows", ignoreCase = true) -> OperatingSystem.Windows
        platformName.contains("linux", ignoreCase = true) -> OperatingSystem.Linux
        else -> OperatingSystem.Unknown
    }

    override val showExtendedAboutDialogCheckbox: Boolean =
        operatingSystem == OperatingSystem.MacOS
}
