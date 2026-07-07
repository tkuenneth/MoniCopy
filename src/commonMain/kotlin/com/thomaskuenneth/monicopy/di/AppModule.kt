package com.thomaskuenneth.monicopy.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [JvmDiModule::class])
@ComponentScan(
    "com.thomaskuenneth.monicopy.app",
    "com.thomaskuenneth.monicopy.copy",
)
class AppModule
