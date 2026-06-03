package su.afk.yummy.tv.feature.settings.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import su.afk.yummy.tv.feature.settings.navigator.SettingsNavRegistrar
import su.afk.yummy.tv.feature.settings.navigator.SettingsNavigator

@Module
@InstallIn(SingletonComponent::class)
interface SettingsNavModule {
    @Binds
    @IntoSet
    fun bindSettingsNavRegistrar(impl: SettingsNavRegistrar): NavRegistrar

    @Binds
    fun bindSettingsNavigator(impl: SettingsNavigator): ISettingsNavigator
}
