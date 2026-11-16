package su.afk.yummy.tv.feature.commonscreen.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.commonscreen.handler.ImageSaver
import su.afk.yummy.tv.feature.commonscreen.handler.MediaStoreImageSaver

@Module
@InstallIn(SingletonComponent::class)
internal interface ImageSaverModule {

    @Binds
    fun bindImageSaver(impl: MediaStoreImageSaver): ImageSaver
}
