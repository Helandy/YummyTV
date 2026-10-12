package su.afk.yummy.tv.feature.player.common.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.player.common.deeplink.LocalVideoDeepLinkResolver

@Module
@InstallIn(SingletonComponent::class)
internal interface PlayerDeepLinkModule {

    @Binds
    @IntoSet
    fun bindLocalVideoDeepLinkResolver(impl: LocalVideoDeepLinkResolver): DeepLinkResolver
}
