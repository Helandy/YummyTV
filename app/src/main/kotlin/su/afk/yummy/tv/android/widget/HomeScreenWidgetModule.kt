package su.afk.yummy.tv.android.widget

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.common.widget.HomeScreenWidgetRefresher

@Module
@InstallIn(SingletonComponent::class)
internal interface HomeScreenWidgetModule {

    @Binds
    fun bindHomeScreenWidgetRefresher(
        impl: GlanceHomeScreenWidgetRefresher,
    ): HomeScreenWidgetRefresher
}
