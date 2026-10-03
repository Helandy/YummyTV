package su.afk.yummy.tv.data.update.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.data.update.apk.ApkDownloaderImpl
import su.afk.yummy.tv.data.update.apk.ApkInstallerImpl
import su.afk.yummy.tv.data.update.repository.GitHubUpdateRepository
import su.afk.yummy.tv.data.update.repository.ServiceUpdateDownloadRepository
import su.afk.yummy.tv.domain.update.repository.ApkDownloadRepository
import su.afk.yummy.tv.domain.update.repository.ApkInstallRepository
import su.afk.yummy.tv.domain.update.repository.UpdateDownloadRepository
import su.afk.yummy.tv.domain.update.repository.UpdateRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface UpdateDataModule {

    @Binds
    @Singleton
    fun bindUpdateRepository(impl: GitHubUpdateRepository): UpdateRepository

    @Binds
    @Singleton
    fun bindUpdateDownloadRepository(impl: ServiceUpdateDownloadRepository): UpdateDownloadRepository

    @Binds
    @Singleton
    fun bindApkDownloader(impl: ApkDownloaderImpl): ApkDownloadRepository

    @Binds
    @Singleton
    fun bindApkInstaller(impl: ApkInstallerImpl): ApkInstallRepository
}
