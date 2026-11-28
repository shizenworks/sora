package com.mochyy.mochymusic.di

import com.mochyy.mochymusic.viewmodels.UserProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The Mochy page's profile ViewModel: it only reads the local library, so every platform can build it. */
val profileModule = module {
    viewModelOf(::UserProfileViewModel)
}
