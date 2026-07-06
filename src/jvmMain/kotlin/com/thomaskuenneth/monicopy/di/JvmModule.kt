package com.thomaskuenneth.monicopy.di

import com.thomaskuenneth.monicopy.app.AppRepository
import com.thomaskuenneth.monicopy.app.JvmAppRepository
import com.thomaskuenneth.monicopy.copy.CopyEngine
import com.thomaskuenneth.monicopy.copy.CopyRepository
import com.thomaskuenneth.monicopy.copy.JvmCopyEngine
import com.thomaskuenneth.monicopy.copy.JvmCopyRepository
import com.thomaskuenneth.monicopy.platform.DirectoryChooser
import com.thomaskuenneth.monicopy.platform.JvmDirectoryChooser
import com.thomaskuenneth.monicopy.platform.JvmLogTimeFormatter
import com.thomaskuenneth.monicopy.platform.JvmPlatformInfo
import com.thomaskuenneth.monicopy.platform.LogTimeFormatter
import com.thomaskuenneth.monicopy.platform.PlatformInfo
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val jvmModule = module {
    singleOf(::JvmAppRepository) { bind<AppRepository>() }
    singleOf(::JvmCopyRepository) { bind<CopyRepository>() }
    singleOf(::JvmCopyEngine) { bind<CopyEngine>() }
    singleOf(::JvmPlatformInfo) { bind<PlatformInfo>() }
    singleOf(::JvmDirectoryChooser) { bind<DirectoryChooser>() }
    singleOf(::JvmLogTimeFormatter) { bind<LogTimeFormatter>() }
}
