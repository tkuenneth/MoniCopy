package com.thomaskuenneth.monicopy.di

import com.thomaskuenneth.monicopy.app.AppViewModel
import com.thomaskuenneth.monicopy.copy.CopyViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::CopyViewModel)
}
