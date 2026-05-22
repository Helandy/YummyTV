package su.afk.yummy.tv.feature.commonscreen.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.commonscreen.errorScreen.ErrorViewModel

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ErrorNavigatorEntryPoint {
    fun creatorErrorViewModelFactory(): ErrorViewModel.Factory
}