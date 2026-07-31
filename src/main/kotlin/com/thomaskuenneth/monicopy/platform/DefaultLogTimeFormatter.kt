package com.thomaskuenneth.monicopy.platform

import org.koin.core.annotation.Single
import java.text.DateFormat
import java.util.Date

@Single
class DefaultLogTimeFormatter : LogTimeFormatter {
    override fun format(): String = DateFormat.getTimeInstance().format(Date())
}
