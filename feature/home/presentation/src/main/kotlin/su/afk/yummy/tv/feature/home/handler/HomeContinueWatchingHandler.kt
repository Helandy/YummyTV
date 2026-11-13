package su.afk.yummy.tv.feature.home.handler

import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentUserId
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.ObserveHomeContinueWatchingUseCase
import su.afk.yummy.tv.domain.home.usecase.RemoveCachedContinueWatchingUseCase
import su.afk.yummy.tv.domain.watching.usecase.ResolveContinueWatchingLaunchUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/** Источник и операции блока «Продолжить просмотр». */
internal class HomeContinueWatchingHandler @Inject constructor(
    private val observeContinueWatching: ObserveHomeContinueWatchingUseCase,
    private val removeCachedContinueWatching: RemoveCachedContinueWatchingUseCase,
    private val resolveContinueWatchingLaunch: ResolveContinueWatchingLaunchUseCase,
    private val settingsStore: SettingsStore,
) {
    fun observe() = observeContinueWatching()

    /** Ждёт окно отмены и только затем убирает запись насовсем; отмена корутины отменяет удаление. */
    suspend fun commitRemoval(animeId: Int) {
        delay(UNDO_WINDOW)
        runSuspendCatching { removeCachedContinueWatching(animeId) }
    }

    /** Цель запуска плеера для [entry]; для авторизованного заодно подтягивает удалённый прогресс. */
    suspend fun resolveLaunch(entry: HomeContinueWatchingItem) = resolveContinueWatchingLaunch(
        entry = entry,
        refreshProgressOnLaunch = settingsStore.currentUserId() > 0,
    )

    private companion object {
        /** Окно отмены удаления — чуть дольше короткого снекбара, чтобы кнопка успела сработать. */
        val UNDO_WINDOW = 5_000.milliseconds
    }
}
