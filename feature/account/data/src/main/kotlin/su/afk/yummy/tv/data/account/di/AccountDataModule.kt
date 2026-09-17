package su.afk.yummy.tv.data.account.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.data.account.localauth.LocalAuthServer
import su.afk.yummy.tv.data.account.localauth.NsdAdvertiser
import su.afk.yummy.tv.data.account.localauth.NsdDeviceDiscovery
import su.afk.yummy.tv.data.account.localauth.SessionTransferClient
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.repository.DefaultAccountMutationErrorNotifier
import su.afk.yummy.tv.data.account.repository.NsdLocalAuthRepository
import su.afk.yummy.tv.data.account.repository.YaniAccountRepository
import su.afk.yummy.tv.data.account.repository.YaniAnimeExtrasRepository
import su.afk.yummy.tv.data.account.repository.YaniProfileNotificationsRepository
import su.afk.yummy.tv.data.account.repository.YaniProfileSettingsRepository
import su.afk.yummy.tv.data.account.repository.YaniUserDirectoryRepository
import su.afk.yummy.tv.data.account.repository.YaniUserListsRepository
import su.afk.yummy.tv.data.account.repository.YaniUserProfileContentRepository
import su.afk.yummy.tv.data.account.repository.YaniUserProfileRepository
import su.afk.yummy.tv.data.account.repository.YaniUserStatsRepository
import su.afk.yummy.tv.data.account.repository.YaniVideoSubscriptionRepository
import su.afk.yummy.tv.data.account.repository.YaniVideoWatchesRepository
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import su.afk.yummy.tv.domain.account.repository.LocalAuthRepository
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import su.afk.yummy.tv.domain.account.repository.ProfileSettingsRepository
import su.afk.yummy.tv.domain.account.repository.UserDirectoryRepository
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import su.afk.yummy.tv.domain.account.repository.UserProfileContentRepository
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
    fun provideYaniAccountApi(
        clientProvider: YaniHttpClientProvider,
        analyticsTracker: AnalyticsTracker,
    ): YaniAccountApi =
        YaniAccountApi(clientProvider, analyticsTracker)

    @Provides
    @Singleton
    fun provideAccountMutationErrorNotifier(): AccountMutationErrorNotifier =
        DefaultAccountMutationErrorNotifier()

    @Provides
    @Singleton
    fun provideAccountRepository(
        api: YaniAccountApi,
        settingsStore: YaniAccountSettingsStore,
        yaniAuthPreferences: YaniAuthPreferences,
        accountStorage: AccountStorage,
        documentCache: DocumentCacheStorage,
        animeStorage: AnimeStorage,
    ): AccountRepository = YaniAccountRepository(
        api,
        settingsStore,
        yaniAuthPreferences,
        accountStorage,
        documentCache,
        animeStorage,
    )

    @Provides
    @Singleton
    fun provideUserListsRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
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
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
    ): AnimeExtrasRepository = YaniAnimeExtrasRepository(
        api,
        accountStorage,
        settingsStore,
    )

    @Provides
    @Singleton
    fun provideVideoSubscriptionRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
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
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
    ): UserStatsRepository =
        YaniUserStatsRepository(api, accountStorage, settingsStore)

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
    ): UserProfileRepository =
        YaniUserProfileRepository(api, accountStorage, settingsStore)

    @Provides
    @Singleton
    fun provideUserProfileContentRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
    ): UserProfileContentRepository =
        YaniUserProfileContentRepository(api, accountStorage, settingsStore)

    @Provides
    @Singleton
    fun provideProfileNotificationsRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
    ): ProfileNotificationsRepository =
        YaniProfileNotificationsRepository(
            api,
            accountStorage,
            settingsStore,
        )

    @Provides
    @Singleton
    fun provideUserDirectoryRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
    ): UserDirectoryRepository =
        YaniUserDirectoryRepository(api, accountStorage, settingsStore)

    @Provides
    @Singleton
    fun provideProfileSettingsRepository(
        api: YaniAccountApi,
        accountRepository: AccountRepository,
        yaniAuthPreferences: YaniAuthPreferences,
    ): ProfileSettingsRepository =
        YaniProfileSettingsRepository(api, accountRepository, yaniAuthPreferences)

    @Provides
    @Singleton
    internal fun provideLocalAuthRepository(
        server: LocalAuthServer,
        advertiser: NsdAdvertiser,
        discovery: NsdDeviceDiscovery,
        transferClient: SessionTransferClient,
    ): LocalAuthRepository = NsdLocalAuthRepository(
        server,
        advertiser,
        discovery,
        transferClient,
    )
}
