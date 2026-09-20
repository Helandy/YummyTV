package su.afk.yummy.tv.core.preferences.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.preferences.auth.SecureYaniAuthPreferences
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceModePreferences
import su.afk.yummy.tv.core.preferences.interface_mode.SharedPreferencesAppInterfaceModePreferences
import su.afk.yummy.tv.core.preferences.settings.AppLifecycleSettingsStore
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.core.preferences.settings.CacheSettingsStore
import su.afk.yummy.tv.core.preferences.settings.EpisodePushSettingsStore
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SearchSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.VideoExportSettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.datastore.DataStoreAppLifecycleSettingsStore
import su.afk.yummy.tv.core.preferences.settings.datastore.DataStoreAppearanceSettingsStore
import su.afk.yummy.tv.core.preferences.settings.datastore.DataStoreCacheSettingsStore
import su.afk.yummy.tv.core.preferences.settings.datastore.DataStoreEpisodePushSettingsStore
import su.afk.yummy.tv.core.preferences.settings.datastore.DataStorePlayerSettingsStore
import su.afk.yummy.tv.core.preferences.settings.datastore.DataStoreSearchSettingsStore
import su.afk.yummy.tv.core.preferences.settings.datastore.DataStoreSettingsStore
import su.afk.yummy.tv.core.preferences.settings.datastore.DataStoreVideoExportSettingsStore
import su.afk.yummy.tv.core.preferences.settings.datastore.DataStoreYaniAccountSettingsStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PreferencesModule {

    @Binds
    @Singleton
    fun bindSettingsStore(impl: DataStoreSettingsStore): SettingsStore

    @Binds
    @Singleton
    fun bindYaniAccountSettingsStore(impl: DataStoreYaniAccountSettingsStore): YaniAccountSettingsStore

    @Binds
    @Singleton
    fun bindPlayerSettingsStore(impl: DataStorePlayerSettingsStore): PlayerSettingsStore

    @Binds
    @Singleton
    fun bindAppearanceSettingsStore(impl: DataStoreAppearanceSettingsStore): AppearanceSettingsStore

    @Binds
    @Singleton
    fun bindCacheSettingsStore(impl: DataStoreCacheSettingsStore): CacheSettingsStore

    @Binds
    @Singleton
    fun bindVideoExportSettingsStore(impl: DataStoreVideoExportSettingsStore): VideoExportSettingsStore

    @Binds
    @Singleton
    fun bindAppLifecycleSettingsStore(
        impl: DataStoreAppLifecycleSettingsStore,
    ): AppLifecycleSettingsStore

    @Binds
    @Singleton
    fun bindAppInterfaceModePreferences(
        impl: SharedPreferencesAppInterfaceModePreferences,
    ): AppInterfaceModePreferences

    @Binds
    @Singleton
    fun bindYaniAuthPreferences(impl: SecureYaniAuthPreferences): YaniAuthPreferences

    @Binds
    @Singleton
    fun bindSearchSettingsStore(impl: DataStoreSearchSettingsStore): SearchSettingsStore

    @Binds
    @Singleton
    fun bindEpisodePushSettingsStore(impl: DataStoreEpisodePushSettingsStore): EpisodePushSettingsStore
}
