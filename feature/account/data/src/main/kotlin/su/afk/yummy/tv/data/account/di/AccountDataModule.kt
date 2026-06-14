package su.afk.yummy.tv.data.account.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.repository.DefaultAccountMutationErrorNotifier
import su.afk.yummy.tv.data.account.repository.YaniAccountRepository
import su.afk.yummy.tv.data.account.repository.YaniAnimeExtrasRepository
import su.afk.yummy.tv.data.account.repository.YaniProfileNotificationsRepository
import su.afk.yummy.tv.data.account.repository.YaniUserListsRepository
import su.afk.yummy.tv.data.account.repository.YaniUserProfileRepository
import su.afk.yummy.tv.data.account.repository.YaniUserStatsRepository
import su.afk.yummy.tv.data.account.repository.YaniVideoSubscriptionRepository
import su.afk.yummy.tv.data.account.repository.YaniVideoWatchesRepository
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import su.afk.yummy.tv.domain.account.repository.UserProfileRepository
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
        accountStorage: AccountStorageStore,
    ): AccountRepository = YaniAccountRepository(
        api,
        settingsStore,
        yaniAuthPreferences,
        accountStorage,
    )

    @Provides
    @Singleton
    fun provideUserListsRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorageStore,
        settingsStore: SettingsStore,
    ): UserListsRepository = YaniUserListsRepository(
        api,
        accountStorage,
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
        accountStorage: AccountStorageStore,
        settingsStore: SettingsStore,
    ): AnimeExtrasRepository = YaniAnimeExtrasRepository(
        api,
        accountStorage,
        settingsStore,
    )

    @Provides
    @Singleton
    fun provideVideoSubscriptionRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorageStore,
        settingsStore: SettingsStore,
    ): VideoSubscriptionRepository =
        YaniVideoSubscriptionRepository(
            api,
            accountStorage,
            settingsStore,
        )

    @Provides
    @Singleton
    fun provideUserStatsRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorageStore,
        settingsStore: SettingsStore,
    ): UserStatsRepository =
        YaniUserStatsRepository(api, accountStorage, settingsStore)

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorageStore,
        settingsStore: SettingsStore,
    ): UserProfileRepository =
        YaniUserProfileRepository(api, accountStorage, settingsStore)

    @Provides
    @Singleton
    fun provideProfileNotificationsRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorageStore,
        settingsStore: SettingsStore,
    ): ProfileNotificationsRepository =
        YaniProfileNotificationsRepository(
            api,
            accountStorage,
            settingsStore,
        )
}
