package com.thomaskuenneth.monicopy.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan(
    "com.thomaskuenneth.monicopy.app",
    "com.thomaskuenneth.monicopy.copy",
    "com.thomaskuenneth.monicopy.platform",
)
class AppModule
