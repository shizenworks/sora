package com.mochyy.mochymusic.di

import com.mochyy.mochymusic.viewmodels.AccountImportViewModel
import com.mochyy.mochymusic.viewmodels.AccountSettingsViewModel
import com.mochyy.mochymusic.viewmodels.AccountSwitcherViewModel
import com.mochyy.mochymusic.viewmodels.AccountViewModel
import com.mochyy.mochymusic.viewmodels.ImportSyncViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The import and sync settings' view models. Their platform contracts (the import service, file
 * access, the saved-accounts store, the sign-in web view) are bound by the app in
 * app/.../di/koin/ImportSyncKoin.kt; a platform that doesn't bind them never opens these pages.
 */
val importSyncModule: Module = module {
    viewModelOf(::ImportSyncViewModel)
    viewModelOf(::AccountImportViewModel)
    viewModelOf(::AccountSwitcherViewModel)
    viewModelOf(::AccountSettingsViewModel)
    viewModelOf(::AccountViewModel)
}
