package su.afk.yummy.tv.data.videodownload.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.data.videodownload.repository.DefaultVideoDownloadRepository
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface VideoDownloadDataModule {
    @Binds
    @Singleton
    fun bindVideoDownloadRepository(
        repository: DefaultVideoDownloadRepository,
    ): VideoDownloadRepository
}
