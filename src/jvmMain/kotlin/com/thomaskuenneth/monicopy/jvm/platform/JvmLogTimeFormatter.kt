package com.thomaskuenneth.monicopy.jvm.platform

import com.thomaskuenneth.monicopy.platform.LogTimeFormatter
import org.koin.core.annotation.Single
import java.text.DateFormat
import java.util.Date

@Single
class JvmLogTimeFormatter : LogTimeFormatter {
    override fun format(): String = DateFormat.getTimeInstance().format(Date())
}
