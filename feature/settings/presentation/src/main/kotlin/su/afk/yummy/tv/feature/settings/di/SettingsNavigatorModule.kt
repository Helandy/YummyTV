package su.afk.yummy.tv.feature.settings.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import su.afk.yummy.tv.feature.settings.navigator.SettingsNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsNavigatorModule {

    @Binds
    @Singleton
    fun bindSettingsNavigator(impl: SettingsNavigator): ISettingsNavigator
}
