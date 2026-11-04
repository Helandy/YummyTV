package su.afk.yummy.tv.feature.glossary.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.glossary.IGlossaryNavigator
import su.afk.yummy.tv.feature.glossary.IMobileGlossaryEntry
import su.afk.yummy.tv.feature.glossary.mobile.navigator.GlossaryNavRegistrar
import su.afk.yummy.tv.feature.glossary.navigator.GlossaryNavigator
import javax.inject.Singleton

// Как и у FAQ, отдельного presentation-DI нет: навигатор биндится рядом с регистратором.
@Module
@InstallIn(SingletonComponent::class)
interface GlossaryMobileNavigationModule {

    @Binds
    fun bindGlossaryNavRegistrar(impl: GlossaryNavRegistrar): IMobileGlossaryEntry

    @Binds
    @Singleton
    fun bindGlossaryNavigator(impl: GlossaryNavigator): IGlossaryNavigator
}
