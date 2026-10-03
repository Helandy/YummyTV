package su.afk.yummy.tv.data.account.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.core.utils.coroutines.AppDispatchers
import su.afk.yummy.tv.data.account.backup.AuthTokenBackup
import su.afk.yummy.tv.data.account.backup.BlockStoreAuthTokenBackup
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
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
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
    fun provideAccountMutationErrorNotifier(): AccountMutationErrorRepository =
        DefaultAccountMutationErrorNotifier()

    @Provides
    @Singleton
    fun provideAuthTokenBackup(
        @ApplicationContext context: Context,
        analyticsTracker: AnalyticsTracker,
    ): AuthTokenBackup = BlockStoreAuthTokenBackup(context, analyticsTracker)

    @Provides
    @Singleton
    fun provideAccountRepository(
        api: YaniAccountApi,
        settingsStore: YaniAccountSettingsStore,
        yaniAuthPreferences: YaniAuthPreferences,
        accountStorage: AccountStorage,
        documentCache: DocumentCacheStorage,
        animeStorage: AnimeStorage,
        analyticsTracker: AnalyticsTracker,
        authTokenBackup: AuthTokenBackup,
        dispatchers: AppDispatchers,
    ): AccountRepository = YaniAccountRepository(
        api,
        settingsStore,
        yaniAuthPreferences,
        accountStorage,
        documentCache,
        animeStorage,
        analyticsTracker,
        authTokenBackup,
        dispatchers,
    )

    @Provides
    @Singleton
    fun provideUserListsRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
        dispatchers: AppDispatchers,
    ): UserListsRepository = YaniUserListsRepository(
        api,
        accountStorage,
        settingsStore,
        dispatchers,
    )

    @Provides
    @Singleton
    fun provideVideoWatchesRepository(
        api: YaniAccountApi,
        dispatchers: AppDispatchers,
    ): VideoWatchesRepository = YaniVideoWatchesRepository(
        api,
        dispatchers,
    )

    @Provides
    @Singleton
    fun provideAnimeExtrasRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
        dispatchers: AppDispatchers,
    ): AnimeExtrasRepository = YaniAnimeExtrasRepository(
        api,
        accountStorage,
        settingsStore,
        dispatchers,
    )

    @Provides
    @Singleton
    fun provideVideoSubscriptionRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
        dispatchers: AppDispatchers,
    ): VideoSubscriptionRepository =
        YaniVideoSubscriptionRepository(
            api,
            accountStorage,
            settingsStore,
            dispatchers,
        )

    @Provides
    @Singleton
    fun provideUserStatsRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
        dispatchers: AppDispatchers,
    ): UserStatsRepository =
        YaniUserStatsRepository(api, accountStorage, settingsStore, dispatchers)

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
        dispatchers: AppDispatchers,
    ): UserProfileRepository =
        YaniUserProfileRepository(api, accountStorage, settingsStore, dispatchers)

    @Provides
    @Singleton
    fun provideUserProfileContentRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
        dispatchers: AppDispatchers,
    ): UserProfileContentRepository =
        YaniUserProfileContentRepository(api, accountStorage, settingsStore, dispatchers)

    @Provides
    @Singleton
    fun provideProfileNotificationsRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
        dispatchers: AppDispatchers,
    ): ProfileNotificationsRepository =
        YaniProfileNotificationsRepository(
            api,
            accountStorage,
            settingsStore,
            dispatchers,
        )

    @Provides
    @Singleton
    fun provideUserDirectoryRepository(
        api: YaniAccountApi,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
        dispatchers: AppDispatchers,
    ): UserDirectoryRepository =
        YaniUserDirectoryRepository(api, accountStorage, settingsStore, dispatchers)

    @Provides
    @Singleton
    fun provideProfileSettingsRepository(
        api: YaniAccountApi,
        accountRepository: AccountRepository,
        yaniAuthPreferences: YaniAuthPreferences,
        authTokenBackup: AuthTokenBackup,
        dispatchers: AppDispatchers,
    ): ProfileSettingsRepository =
        YaniProfileSettingsRepository(api, accountRepository, yaniAuthPreferences, authTokenBackup, dispatchers)

    /** Таймауты короткие: ТВ стоит в той же сети, долгого ожидания тут быть не должно. */
    @Provides
    @Singleton
    @LocalAuthHttpClient
    internal fun provideLocalAuthHttpClient(json: Json): HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = LOCAL_AUTH_CONNECT_TIMEOUT_MS
            requestTimeoutMillis = LOCAL_AUTH_REQUEST_TIMEOUT_MS
        }
        install(ContentNegotiation) { json(json) }
    }

    @Provides
    @Singleton
    internal fun provideLocalAuthRepository(
        server: LocalAuthServer,
        advertiser: NsdAdvertiser,
        discovery: NsdDeviceDiscovery,
        transferClient: SessionTransferClient,
        dispatchers: AppDispatchers,
    ): LocalAuthRepository = NsdLocalAuthRepository(
        server,
        advertiser,
        discovery,
        transferClient,
        dispatchers,
    )

    private const val LOCAL_AUTH_CONNECT_TIMEOUT_MS = 5_000L
    private const val LOCAL_AUTH_REQUEST_TIMEOUT_MS = 10_000L
}
