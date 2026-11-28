package com.mochyy.mochymusic.di

import com.mochyy.mochymusic.viewmodels.CloudBackupViewModel
import com.mochyy.mochymusic.viewmodels.IntegrationsViewModel
import com.mochyy.mochymusic.viewmodels.StorageViewModel
import com.mochyy.mochymusic.viewmodels.TransferViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The settings pages whose work belongs to the platform: storage, cloud backup and scrobbling.
 * Each view model only needs its platform contract and the one preferences store, so the app's
 * module adds the Android implementations and no platform-specific type reaches commonMain.
 */
val storageModule = module {
    viewModelOf(::StorageViewModel)
    viewModelOf(::CloudBackupViewModel)
    viewModelOf(::IntegrationsViewModel)
    viewModelOf(::TransferViewModel)
}
