package su.afk.yummy.tv.feature.commonscreen.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.commonscreen.imageView.ImageViewViewModel

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ImageViewNavigatorEntryPoint {
    fun imageViewViewModelFactory(): ImageViewViewModel.Factory
}
