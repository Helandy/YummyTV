package su.afk.yummy.tv.core.update.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import su.afk.yummy.tv.core.update.apk.ApkDownloader
import su.afk.yummy.tv.core.update.apk.ApkInstaller
import su.afk.yummy.tv.core.update.github.GitHubUpdateChecker
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {

    @Provides @Singleton
    fun provideGitHubUpdateChecker(client: HttpClient): GitHubUpdateChecker =
        GitHubUpdateChecker(client)

    @Provides @Singleton
    fun provideApkDownloader(@ApplicationContext context: Context): ApkDownloader =
        ApkDownloader(context)

    @Provides @Singleton
    fun provideApkInstaller(@ApplicationContext context: Context): ApkInstaller =
        ApkInstaller(context)

    @Provides @Named("appVersionName")
    fun provideVersionName(@ApplicationContext context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
}
