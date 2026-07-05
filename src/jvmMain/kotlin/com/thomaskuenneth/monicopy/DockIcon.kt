package com.thomaskuenneth.monicopy

import java.awt.Taskbar
import javax.imageio.ImageIO

fun setupDockIcon() {
    if (!Taskbar.isTaskbarSupported()) return
    val taskbar = Taskbar.getTaskbar()
    if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return
    val image = Thread.currentThread().contextClassLoader
        .getResourceAsStream("app_icon.png")
        ?.use(ImageIO::read)
        ?: return
    taskbar.iconImage = image
}
