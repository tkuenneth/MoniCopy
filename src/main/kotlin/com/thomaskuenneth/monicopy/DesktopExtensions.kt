package com.thomaskuenneth.monicopy

import java.awt.Desktop
import java.awt.desktop.AboutHandler
import java.awt.desktop.PreferencesHandler

fun Desktop.installPreferencesHandler(handler: PreferencesHandler) {
    if (isSupported(Desktop.Action.APP_PREFERENCES)) {
        setPreferencesHandler(handler)
    }
}

fun Desktop.installAboutHandler(handler: AboutHandler?) {
    if (isSupported(Desktop.Action.APP_ABOUT)) {
        setAboutHandler(handler)
    }
}
