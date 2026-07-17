package su.afk.yummy.tv.data.pages.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.data.pages.repository.YaniSitePagesRepository
import su.afk.yummy.tv.domain.pages.repository.SitePagesRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class SitePagesDataModule {
    @Binds
    abstract fun bindSitePagesRepository(impl: YaniSitePagesRepository): SitePagesRepository
}
