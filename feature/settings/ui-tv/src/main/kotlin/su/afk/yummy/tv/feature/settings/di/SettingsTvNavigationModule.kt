package su.afk.yummy.tv.feature.settings.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.settings.ITvSettingsEntry
import su.afk.yummy.tv.feature.settings.tv.navigator.SettingsNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface SettingsTvNavigationModule {

    @Binds
    fun bindSettingsNavRegistrar(impl: SettingsNavRegistrar): ITvSettingsEntry
}
