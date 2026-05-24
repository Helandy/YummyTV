package su.afk.yummy.tv.data.account.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.data.account.YaniAccountRepository
import su.afk.yummy.tv.data.account.YaniAnimeExtrasRepository
import su.afk.yummy.tv.data.account.YaniUserListsRepository
import su.afk.yummy.tv.data.account.YaniVideoSubscriptionRepository
import su.afk.yummy.tv.data.account.YaniVideoWatchesRepository
import su.afk.yummy.tv.domain.account.AccountRepository
import su.afk.yummy.tv.domain.account.AnimeExtrasRepository
import su.afk.yummy.tv.domain.account.LoginUseCase
import su.afk.yummy.tv.domain.account.LogoutUseCase
import su.afk.yummy.tv.domain.account.RefreshAccountUseCase
import su.afk.yummy.tv.domain.account.UserListsRepository
import su.afk.yummy.tv.domain.account.VideoSubscriptionRepository
import su.afk.yummy.tv.domain.account.VideoWatchesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountDataModule {

    @Provides
    @Singleton
    fun provideAccountRepository(client: HttpClient, settingsStore: SettingsStore): AccountRepository =
        YaniAccountRepository(client, settingsStore)

    @Provides
    @Singleton
    fun provideUserListsRepository(client: HttpClient): UserListsRepository = YaniUserListsRepository(client)

    @Provides
    @Singleton
    fun provideVideoWatchesRepository(client: HttpClient): VideoWatchesRepository = YaniVideoWatchesRepository(client)

    @Provides
    @Singleton
    fun provideAnimeExtrasRepository(client: HttpClient): AnimeExtrasRepository = YaniAnimeExtrasRepository(client)

    @Provides
    @Singleton
    fun provideVideoSubscriptionRepository(client: HttpClient): VideoSubscriptionRepository =
        YaniVideoSubscriptionRepository(client)

    @Provides
    fun provideLoginUseCase(repository: AccountRepository) = LoginUseCase(repository)

    @Provides
    fun provideLogoutUseCase(repository: AccountRepository) = LogoutUseCase(repository)

    @Provides
    fun provideRefreshAccountUseCase(repository: AccountRepository) = RefreshAccountUseCase(repository)
}
