package su.afk.yummy.tv.feature.videodownload.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.videodownload.IMobileVideoDownloadEntry
import su.afk.yummy.tv.feature.videodownload.mobile.navigator.VideoDownloadNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface VideoDownloadMobileNavigationModule {

    @Binds
    fun bindVideoDownloadNavRegistrar(impl: VideoDownloadNavRegistrar): IMobileVideoDownloadEntry
}
