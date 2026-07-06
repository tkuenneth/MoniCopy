package com.thomaskuenneth.monicopy.platform

interface PlatformInfo {
    val platformName: String
    val appVersion: String
    val operatingSystem: OperatingSystem
    val showExtendedAboutDialogCheckbox: Boolean
}
