package su.afk.yummy.tv.android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.android.startup.ImageFormatProbe
import su.afk.yummy.tv.core.model.settings.ImageFormatSupport

/** Проба форматов живёт в app (там libavif), а потребляется фичами через интерфейс из core:model. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ImageFormatSupportModule {

    @Binds
    abstract fun bindImageFormatSupport(probe: ImageFormatProbe): ImageFormatSupport
}
