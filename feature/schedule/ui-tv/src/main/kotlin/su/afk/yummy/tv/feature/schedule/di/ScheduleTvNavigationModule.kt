package su.afk.yummy.tv.feature.schedule.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.schedule.ITvScheduleEntry
import su.afk.yummy.tv.feature.schedule.tv.navigator.ScheduleNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface ScheduleTvNavigationModule {

    @Binds
    fun bindScheduleNavRegistrar(impl: ScheduleNavRegistrar): ITvScheduleEntry
}
