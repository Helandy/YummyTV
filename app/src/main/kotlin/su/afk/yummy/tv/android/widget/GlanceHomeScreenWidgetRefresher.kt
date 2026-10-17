package su.afk.yummy.tv.android.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.common.widget.HomeScreenWidgetRefresher
import javax.inject.Inject

internal class GlanceHomeScreenWidgetRefresher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : HomeScreenWidgetRefresher {

    override suspend fun refresh() {
        // Сбой виджета не должен ронять фоновую задачу, которая его дёрнула.
        runSuspendCatching { ContinueWatchingWidget().updateAll(context) }
    }
}
