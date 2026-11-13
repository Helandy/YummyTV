package su.afk.yummy.tv.android.image.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.android.image.CoilImageCacheCleaner
import su.afk.yummy.tv.core.utils.system.ImageCacheCleaner

/** Очистка кэша картинок живёт в app (там собирается Coil), настройки видят только интерфейс из core:utils. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ImageCacheCleanerModule {

    @Binds
    abstract fun bindImageCacheCleaner(cleaner: CoilImageCacheCleaner): ImageCacheCleaner
}
