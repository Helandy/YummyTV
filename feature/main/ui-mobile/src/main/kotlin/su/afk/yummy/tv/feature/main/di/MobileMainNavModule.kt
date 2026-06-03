package su.afk.yummy.tv.feature.main.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.main.MobileMainGraph
import su.afk.yummy.tv.feature.main.api.IMainGraph
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface MobileMainNavModule {

    @Binds
    @Singleton
    fun bindMobileMainGraph(impl: MobileMainGraph): IMainGraph
}
