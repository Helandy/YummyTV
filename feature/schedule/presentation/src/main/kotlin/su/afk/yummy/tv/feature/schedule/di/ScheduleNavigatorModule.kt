package su.afk.yummy.tv.feature.schedule.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator
import su.afk.yummy.tv.feature.schedule.navigator.ScheduleNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ScheduleNavigatorModule {

    @Binds
    @Singleton
    fun bindScheduleNavigator(impl: ScheduleNavigator): IScheduleNavigator
}
