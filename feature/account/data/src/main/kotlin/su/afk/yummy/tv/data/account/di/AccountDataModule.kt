package su.afk.yummy.tv.data.account.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
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
        accountStorage: AccountStorageStore,
        json: Json,
    ): AccountRepository = YaniAccountRepository(
        api,
        settingsStore,
        yaniAuthPreferences,
        cache,
        accountStorage,
        json,
    )

    @Provides
    @Singleton
    fun provideUserListsRepository(
        api: YaniAccountApi,
        cache: CacheStore,
        accountStorage: AccountStorageStore,
        json: Json,
        settingsStore: SettingsStore,
    ): UserListsRepository = YaniUserListsRepository(
        api,
        cache,
        accountStorage,
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
        accountStorage: AccountStorageStore,
        json: Json,
        settingsStore: SettingsStore,
    ): AnimeExtrasRepository = YaniAnimeExtrasRepository(
        api,
        cache,
        accountStorage,
        json,
        settingsStore,
    )

    @Provides
    @Singleton
    fun provideVideoSubscriptionRepository(
        api: YaniAccountApi,
        cache: CacheStore,
        accountStorage: AccountStorageStore,
        json: Json,
        settingsStore: SettingsStore,
    ): VideoSubscriptionRepository =
        YaniVideoSubscriptionRepository(
            api,
            cache,
            accountStorage,
            json,
            settingsStore,
        )

    @Provides
    @Singleton
    fun provideUserStatsRepository(
        api: YaniAccountApi,
        cache: CacheStore,
        accountStorage: AccountStorageStore,
        json: Json,
        settingsStore: SettingsStore,
    ): UserStatsRepository =
        YaniUserStatsRepository(api, cache, accountStorage, json, settingsStore)

    @Provides
    @Singleton
    fun provideProfileNotificationsRepository(
        api: YaniAccountApi,
        cache: CacheStore,
        accountStorage: AccountStorageStore,
        json: Json,
        settingsStore: SettingsStore,
    ): ProfileNotificationsRepository =
        YaniProfileNotificationsRepository(
            api,
            cache,
            accountStorage,
            json,
            settingsStore,
        )
}
