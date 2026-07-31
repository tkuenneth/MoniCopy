package com.thomaskuenneth.monicopy.platform

interface DirectoryChooser {
    fun chooseDirectory(title: String, initialPath: String?): String?
}
