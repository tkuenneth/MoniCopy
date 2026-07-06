package com.thomaskuenneth.monicopy.platform

import java.text.DateFormat
import java.util.Date

class JvmLogTimeFormatter : LogTimeFormatter {
    override fun format(): String = DateFormat.getTimeInstance().format(Date())
}
