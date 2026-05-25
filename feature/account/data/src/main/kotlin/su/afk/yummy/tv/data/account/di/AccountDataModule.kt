package su.afk.yummy.tv.data.account.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.repository.YaniAccountRepository
import su.afk.yummy.tv.data.account.repository.YaniAnimeExtrasRepository
import su.afk.yummy.tv.data.account.repository.YaniUserListsRepository
import su.afk.yummy.tv.data.account.repository.YaniVideoSubscriptionRepository
import su.afk.yummy.tv.data.account.repository.YaniVideoWatchesRepository
import su.afk.yummy.tv.domain.account.AccountRepository
import su.afk.yummy.tv.domain.account.AnimeExtrasRepository
import su.afk.yummy.tv.domain.account.DeleteAnimeRatingUseCase
import su.afk.yummy.tv.domain.account.GetAnimeCollectionsUseCase
import su.afk.yummy.tv.domain.account.GetAnimeListStateUseCase
import su.afk.yummy.tv.domain.account.GetAnimeListStatsUseCase
import su.afk.yummy.tv.domain.account.GetAnimeRatingSummaryUseCase
import su.afk.yummy.tv.domain.account.GetAnimeUserRatingUseCase
import su.afk.yummy.tv.domain.account.GetCollectionsUseCase
import su.afk.yummy.tv.domain.account.GetUserAnimeListUseCase
import su.afk.yummy.tv.domain.account.LoginUseCase
import su.afk.yummy.tv.domain.account.LogoutUseCase
import su.afk.yummy.tv.domain.account.MarkVideoWatchedUseCase
import su.afk.yummy.tv.domain.account.RefreshAccountUseCase
import su.afk.yummy.tv.domain.account.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.RemoveWatchedVideoUseCase
import su.afk.yummy.tv.domain.account.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.SetAnimeListUseCase
import su.afk.yummy.tv.domain.account.SetAnimeRatingUseCase
import su.afk.yummy.tv.domain.account.SetVideoSubscriptionUseCase
import su.afk.yummy.tv.domain.account.SyncWatchedVideosUseCase
import su.afk.yummy.tv.domain.account.UserListsRepository
import su.afk.yummy.tv.domain.account.VideoSubscriptionRepository
import su.afk.yummy.tv.domain.account.VideoWatchesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountDataModule {

    @Provides
    @Singleton
    fun provideYaniAccountApi(client: HttpClient): YaniAccountApi = YaniAccountApi(client)

    @Provides
    @Singleton
    fun provideAccountRepository(api: YaniAccountApi, settingsStore: SettingsStore): AccountRepository =
        YaniAccountRepository(api, settingsStore)

    @Provides
    @Singleton
    fun provideUserListsRepository(api: YaniAccountApi): UserListsRepository = YaniUserListsRepository(api)

    @Provides
    @Singleton
    fun provideVideoWatchesRepository(api: YaniAccountApi): VideoWatchesRepository = YaniVideoWatchesRepository(api)

    @Provides
    @Singleton
    fun provideAnimeExtrasRepository(api: YaniAccountApi): AnimeExtrasRepository = YaniAnimeExtrasRepository(api)

    @Provides
    @Singleton
    fun provideVideoSubscriptionRepository(api: YaniAccountApi): VideoSubscriptionRepository =
        YaniVideoSubscriptionRepository(api)

    @Provides
    fun provideLoginUseCase(repository: AccountRepository) = LoginUseCase(repository)

    @Provides
    fun provideLogoutUseCase(repository: AccountRepository) = LogoutUseCase(repository)

    @Provides
    fun provideRefreshAccountUseCase(repository: AccountRepository) = RefreshAccountUseCase(repository)

    @Provides
    fun provideGetUserAnimeListUseCase(repository: UserListsRepository) = GetUserAnimeListUseCase(repository)

    @Provides
    fun provideGetAnimeListStateUseCase(repository: UserListsRepository) = GetAnimeListStateUseCase(repository)

    @Provides
    fun provideSetAnimeListUseCase(repository: UserListsRepository) = SetAnimeListUseCase(repository)

    @Provides
    fun provideRemoveAnimeListUseCase(repository: UserListsRepository) = RemoveAnimeListUseCase(repository)

    @Provides
    fun provideSetAnimeFavoriteUseCase(repository: UserListsRepository) = SetAnimeFavoriteUseCase(repository)

    @Provides
    fun provideGetAnimeRatingSummaryUseCase(repository: AnimeExtrasRepository) = GetAnimeRatingSummaryUseCase(repository)

    @Provides
    fun provideGetAnimeUserRatingUseCase(repository: AnimeExtrasRepository) = GetAnimeUserRatingUseCase(repository)

    @Provides
    fun provideSetAnimeRatingUseCase(repository: AnimeExtrasRepository) = SetAnimeRatingUseCase(repository)

    @Provides
    fun provideDeleteAnimeRatingUseCase(repository: AnimeExtrasRepository) = DeleteAnimeRatingUseCase(repository)

    @Provides
    fun provideGetAnimeListStatsUseCase(repository: AnimeExtrasRepository) = GetAnimeListStatsUseCase(repository)

    @Provides
    fun provideGetAnimeCollectionsUseCase(repository: AnimeExtrasRepository) = GetAnimeCollectionsUseCase(repository)

    @Provides
    fun provideGetCollectionsUseCase(repository: AnimeExtrasRepository) = GetCollectionsUseCase(repository)

    @Provides
    fun provideMarkVideoWatchedUseCase(repository: VideoWatchesRepository) = MarkVideoWatchedUseCase(repository)

    @Provides
    fun provideRemoveWatchedVideoUseCase(repository: VideoWatchesRepository) = RemoveWatchedVideoUseCase(repository)

    @Provides
    fun provideSyncWatchedVideosUseCase(repository: VideoWatchesRepository) = SyncWatchedVideosUseCase(repository)

    @Provides
    fun provideSetVideoSubscriptionUseCase(repository: VideoSubscriptionRepository) =
        SetVideoSubscriptionUseCase(repository)
}
