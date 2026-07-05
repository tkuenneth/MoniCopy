package com.thomaskuenneth.monicopy

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

fun blockingGetString(resource: StringResource, vararg formatArgs: Any): String = runBlocking {
    if (formatArgs.isEmpty()) {
        getString(resource)
    } else {
        getString(resource, *formatArgs)
    }
}
