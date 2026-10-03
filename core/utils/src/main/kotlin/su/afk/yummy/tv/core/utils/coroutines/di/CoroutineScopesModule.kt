package su.afk.yummy.tv.core.utils.coroutines.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.core.utils.coroutines.AppDispatchers
import su.afk.yummy.tv.core.utils.coroutines.DefaultAppDispatchers
import su.afk.yummy.tv.core.utils.coroutines.SystemAppClock
import su.afk.yummy.tv.core.utils.coroutines.defaultScope
import su.afk.yummy.tv.core.utils.coroutines.ioScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoroutineScopesModule {

    @Binds
    abstract fun bindAppDispatchers(impl: DefaultAppDispatchers): AppDispatchers

    @Binds
    abstract fun bindAppClock(impl: SystemAppClock): AppClock

    companion object {

        @Provides
        @Singleton
        @IoApplicationScope
        fun provideIoApplicationScope(): CoroutineScope = ioScope()

        @Provides
        @Singleton
        @DefaultApplicationScope
        fun provideDefaultApplicationScope(): CoroutineScope = defaultScope()
    }
}
