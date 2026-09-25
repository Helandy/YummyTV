package su.afk.yummy.tv.feature.videodownload.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator
import su.afk.yummy.tv.feature.videodownload.deeplink.VideoDownloadDeepLinkResolver
import su.afk.yummy.tv.feature.videodownload.navigator.VideoDownloadNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface VideoDownloadNavigatorModule {

    @Binds
    @Singleton
    fun bindVideoDownloadNavigator(impl: VideoDownloadNavigator): IVideoDownloadNavigator

    @Binds
    @IntoSet
    fun bindVideoDownloadDeepLinkResolver(impl: VideoDownloadDeepLinkResolver): DeepLinkResolver
}
