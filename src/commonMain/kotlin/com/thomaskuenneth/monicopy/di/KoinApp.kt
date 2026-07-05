package com.thomaskuenneth.monicopy.di

import org.koin.core.module.Module
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes

fun initKoin(vararg extraModules: Module, config: KoinAppDeclaration? = null) {
    startKoin {
        includes(config)
        modules(appModule, *extraModules)
    }
}
