package su.afk.yummy.tv.core.deeplink.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.deeplink.DeepLinkHandler
import su.afk.yummy.tv.core.deeplink.DeepLinkHandlerImpl
import su.afk.yummy.tv.core.deeplink.DeepLinkResolver
import su.afk.yummy.tv.core.deeplink.YummyDeepLinkResolver
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DeepLinkModule {

    @Binds @Singleton
    fun bindDeepLinkResolver(impl: YummyDeepLinkResolver): DeepLinkResolver

    @Binds @Singleton
    fun bindDeepLinkHandler(impl: DeepLinkHandlerImpl): DeepLinkHandler
}
