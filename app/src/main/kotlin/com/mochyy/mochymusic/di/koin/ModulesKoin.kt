package com.mochyy.mochymusic.di.koin

import android.content.Context
import com.mochyy.mochymusic.modules.ModuleCatalog
import com.mochyy.mochymusic.modules.ModuleRegistry
import com.mochyy.mochymusic.modules.SourceModules
import com.mochyy.mochymusic.viewmodels.ModulesViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The modules screen's view model, for Koin. The module registry and its catalog are Hilt singletons
 * (they hold the loaded modules, their preferences and the JS bridge), so Koin is handed those same
 * instances instead of building a second set; only the view model itself changes hands.
 */
fun modulesKoinModule(context: Context): Module {
    val application = context.applicationContext
    // looked up on first use, when a screen asks for the view model, not while the app is starting
    val graph by lazy { EntryPointAccessors.fromApplication(application, ModulesGraphEntryPoint::class.java) }
    return module {
        single { graph.sourceModules() }
        single { graph.moduleRegistry() }
        single { graph.moduleCatalog() }
        viewModel { ModulesViewModel(application, get(), get(), get()) }
    }
}

/** One line in App.onCreate, next to the other Android Koin bindings. */
fun loadModulesKoin(context: Context) {
    org.koin.core.context.loadKoinModules(modulesKoinModule(context))
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ModulesGraphEntryPoint {
    fun sourceModules(): SourceModules
    fun moduleRegistry(): ModuleRegistry
    fun moduleCatalog(): ModuleCatalog
}
