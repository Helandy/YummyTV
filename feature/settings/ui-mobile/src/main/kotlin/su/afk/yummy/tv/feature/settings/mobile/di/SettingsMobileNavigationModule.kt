package su.afk.yummy.tv.feature.settings.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.settings.IMobileSettingsEntry
import su.afk.yummy.tv.feature.settings.mobile.navigator.SettingsNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface SettingsMobileNavigationModule {

    @Binds
    fun bindSettingsNavRegistrar(impl: SettingsNavRegistrar): IMobileSettingsEntry
}
