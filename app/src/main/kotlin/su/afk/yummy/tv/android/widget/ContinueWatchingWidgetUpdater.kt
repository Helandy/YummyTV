package su.afk.yummy.tv.android.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import javax.inject.Inject

/**
 * Перерисовывает виджеты «Продолжить просмотр», когда меняется список (досмотрели серию,
 * сменили позицию). Список локальный и меняется только в приложении, поэтому периодического
 * обновления у виджета нет — достаточно следить, пока приложение открыто.
 */
class ContinueWatchingWidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val observeContinueWatching: ObserveContinueWatchingUseCase,
) {

    @OptIn(FlowPreview::class)
    suspend fun run() {
        observeContinueWatching()
            // Первое значение не пропускаем: прошлый апдейт мог отмениться вместе с Activity
            // внутри debounce, и виджет остался бы со старым списком.
            .distinctUntilChanged()
            .debounce(UPDATE_DEBOUNCE_MS)
            .collect { runSuspendCatching { ContinueWatchingWidget().updateAll(context) } }
    }

    private companion object {
        const val UPDATE_DEBOUNCE_MS = 2_000L
    }
}
