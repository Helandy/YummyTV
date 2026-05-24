package su.afk.yummy.tv.feature.schedule.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator
import su.afk.yummy.tv.feature.schedule.navigator.ScheduleNavRegistrar
import su.afk.yummy.tv.feature.schedule.navigator.ScheduleNavigator

@Module
@InstallIn(SingletonComponent::class)
interface ScheduleNavModule {
    @Binds
    @IntoSet
    fun bindScheduleNavRegistrar(impl: ScheduleNavRegistrar): NavRegistrar

    @Binds
    fun bindScheduleNavigator(impl: ScheduleNavigator): IScheduleNavigator
}
