package su.afk.yummy.tv.feature.settings.di

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.model.settings.TorrServerChecker

/** Зависимости настроек, которые даёт необязательный модуль: без него `Optional` пуст. */
@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsOptionalModule {

    @BindsOptionalOf
    fun optionalTorrServerChecker(): TorrServerChecker
}
