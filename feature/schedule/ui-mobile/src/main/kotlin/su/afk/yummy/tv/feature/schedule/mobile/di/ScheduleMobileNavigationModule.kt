package su.afk.yummy.tv.feature.schedule.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.schedule.IMobileScheduleEntry
import su.afk.yummy.tv.feature.schedule.mobile.navigator.ScheduleNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface ScheduleMobileNavigationModule {

    @Binds
    fun bindScheduleNavRegistrar(impl: ScheduleNavRegistrar): IMobileScheduleEntry
}
