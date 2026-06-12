package su.afk.yummy.tv.data.account.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.repository.DefaultAccountMutationErrorNotifier
import su.afk.yummy.tv.data.account.repository.YaniAccountRepository
import su.afk.yummy.tv.data.account.repository.YaniAnimeExtrasRepository
import su.afk.yummy.tv.data.account.repository.YaniProfileNotificationsRepository
import su.afk.yummy.tv.data.account.repository.YaniUserListsRepository
import su.afk.yummy.tv.data.account.repository.YaniUserStatsRepository
import su.afk.yummy.tv.data.account.repository.YaniVideoSubscriptionRepository
import su.afk.yummy.tv.data.account.repository.YaniVideoWatchesRepository
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import su.afk.yummy.tv.domain.account.repository.UserStatsRepository
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository
import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository
import su.afk.yummy.tv.domain.account.usecase.DeleteAnimeRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.DeleteNotificationUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeCollectionsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStateUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeRatingSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeUserRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.GetCollectionsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetNotificationCountsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetProfileNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserFavoriteAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetVideoSubscriptionsUseCase
import su.afk.yummy.tv.domain.account.usecase.LoginUseCase
import su.afk.yummy.tv.domain.account.usecase.LogoutUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkAllNotificationsReadUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkNotificationReadUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkVideoWatchedUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveWatchedVideoUseCase
import su.afk.yummy.tv.domain.account.usecase.ResolveNotificationAnimeIdUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.SetVideoSubscriptionUseCase
import su.afk.yummy.tv.domain.account.usecase.SyncWatchedVideosUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountDataModule {

    @Provides
    @Singleton
    fun provideYaniAccountApi(client: HttpClient): YaniAccountApi = YaniAccountApi(client)

    @Provides
    @Singleton
    fun provideAccountMutationErrorNotifier(): AccountMutationErrorNotifier =
        DefaultAccountMutationErrorNotifier()

    @Provides
    @Singleton
    fun provideAccountRepository(
        api: YaniAccountApi,
        settingsStore: SettingsStore,
        yaniAuthPreferences: YaniAuthPreferences,
        cache: CacheStore,
        json: Json,
    ): AccountRepository = YaniAccountRepository(
        api,
        settingsStore,
        yaniAuthPreferences,
        cache,
        json,
    )

    @Provides
    @Singleton
    fun provideUserListsRepository(
        api: YaniAccountApi,
        cache: CacheStore,
        json: Json,
        settingsStore: SettingsStore,
    ): UserListsRepository = YaniUserListsRepository(
        api,
        cache,
        json,
        settingsStore,
    )

    @Provides
    @Singleton
    fun provideVideoWatchesRepository(
        api: YaniAccountApi,
    ): VideoWatchesRepository = YaniVideoWatchesRepository(
        api,
    )

    @Provides
    @Singleton
    fun provideAnimeExtrasRepository(
        api: YaniAccountApi,
        cache: CacheStore,
        json: Json,
        settingsStore: SettingsStore,
    ): AnimeExtrasRepository = YaniAnimeExtrasRepository(
        api,
        cache,
        json,
        settingsStore,
    )

    @Provides
    @Singleton
    fun provideVideoSubscriptionRepository(
        api: YaniAccountApi,
        cache: CacheStore,
        json: Json,
        settingsStore: SettingsStore,
    ): VideoSubscriptionRepository =
        YaniVideoSubscriptionRepository(
            api,
            cache,
            json,
            settingsStore,
        )

    @Provides
    @Singleton
    fun provideUserStatsRepository(
        api: YaniAccountApi,
        cache: CacheStore,
        json: Json,
        settingsStore: SettingsStore,
    ): UserStatsRepository = YaniUserStatsRepository(api, cache, json, settingsStore)

    @Provides
    @Singleton
    fun provideProfileNotificationsRepository(
        api: YaniAccountApi,
        cache: CacheStore,
        json: Json,
        settingsStore: SettingsStore,
    ): ProfileNotificationsRepository =
        YaniProfileNotificationsRepository(
            api,
            cache,
            json,
            settingsStore,
        )

    @Provides
    fun provideLoginUseCase(repository: AccountRepository) = LoginUseCase(repository)

    @Provides
    fun provideLogoutUseCase(repository: AccountRepository) = LogoutUseCase(repository)

    @Provides
    fun provideRefreshAccountUseCase(repository: AccountRepository) = RefreshAccountUseCase(repository)

    @Provides
    fun provideGetUserAnimeListUseCase(repository: UserListsRepository) = GetUserAnimeListUseCase(repository)

    @Provides
    fun provideGetUserFavoriteAnimeListUseCase(repository: UserListsRepository) = GetUserFavoriteAnimeListUseCase(repository)

    @Provides
    fun provideGetAnimeListStateUseCase(repository: UserListsRepository) = GetAnimeListStateUseCase(repository)

    @Provides
    fun provideSetAnimeListUseCase(
        repository: UserListsRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = SetAnimeListUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideRemoveAnimeListUseCase(
        repository: UserListsRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = RemoveAnimeListUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideSetAnimeFavoriteUseCase(
        repository: UserListsRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = SetAnimeFavoriteUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideGetAnimeRatingSummaryUseCase(repository: AnimeExtrasRepository) = GetAnimeRatingSummaryUseCase(repository)

    @Provides
    fun provideGetAnimeUserRatingUseCase(repository: AnimeExtrasRepository) = GetAnimeUserRatingUseCase(repository)

    @Provides
    fun provideSetAnimeRatingUseCase(
        repository: AnimeExtrasRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = SetAnimeRatingUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideDeleteAnimeRatingUseCase(
        repository: AnimeExtrasRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = DeleteAnimeRatingUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideGetAnimeListStatsUseCase(repository: AnimeExtrasRepository) = GetAnimeListStatsUseCase(repository)

    @Provides
    fun provideGetAnimeCollectionsUseCase(repository: AnimeExtrasRepository) = GetAnimeCollectionsUseCase(repository)

    @Provides
    fun provideGetCollectionsUseCase(repository: AnimeExtrasRepository) = GetCollectionsUseCase(repository)

    @Provides
    fun provideMarkVideoWatchedUseCase(
        repository: VideoWatchesRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = MarkVideoWatchedUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideRemoveWatchedVideoUseCase(
        repository: VideoWatchesRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = RemoveWatchedVideoUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideSyncWatchedVideosUseCase(
        repository: VideoWatchesRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = SyncWatchedVideosUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideSetVideoSubscriptionUseCase(
        repository: VideoSubscriptionRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = SetVideoSubscriptionUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideGetVideoSubscriptionsUseCase(repository: VideoSubscriptionRepository) =
        GetVideoSubscriptionsUseCase(repository)

    @Provides
    fun provideGetUserStatsUseCase(repository: UserStatsRepository) = GetUserStatsUseCase(repository)

    @Provides
    fun provideGetProfileNotificationsUseCase(repository: ProfileNotificationsRepository) =
        GetProfileNotificationsUseCase(repository)

    @Provides
    fun provideGetNotificationCountsUseCase(repository: ProfileNotificationsRepository) =
        GetNotificationCountsUseCase(repository)

    @Provides
    fun provideResolveNotificationAnimeIdUseCase(repository: ProfileNotificationsRepository) =
        ResolveNotificationAnimeIdUseCase(repository)

    @Provides
    fun provideMarkNotificationReadUseCase(
        repository: ProfileNotificationsRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = MarkNotificationReadUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideMarkAllNotificationsReadUseCase(
        repository: ProfileNotificationsRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = MarkAllNotificationsReadUseCase(repository, mutationErrorNotifier)

    @Provides
    fun provideDeleteNotificationUseCase(
        repository: ProfileNotificationsRepository,
        mutationErrorNotifier: AccountMutationErrorNotifier,
    ) = DeleteNotificationUseCase(repository, mutationErrorNotifier)
}
