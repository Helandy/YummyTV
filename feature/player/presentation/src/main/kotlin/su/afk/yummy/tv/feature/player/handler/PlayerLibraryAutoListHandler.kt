package su.afk.yummy.tv.feature.player.handler

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.core.model.anime.progress
import su.afk.yummy.tv.core.model.mutation.PendingMutation
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStateUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.library.usecase.RemoveLibraryItemUseCase
import su.afk.yummy.tv.domain.library.usecase.UpsertLibraryItemUseCase
import su.afk.yummy.tv.domain.player.usecase.GetAnimeWatchProgressUseCase
import su.afk.yummy.tv.feature.player.mapper.toLibraryItem
import su.afk.yummy.tv.feature.player.utils.AUTO_WATCHING_EPISODE_PROGRESS
import su.afk.yummy.tv.feature.player.utils.autoLibraryTarget
import su.afk.yummy.tv.feature.player.utils.canAutoMoveTo
import javax.inject.Inject

/**
 * Автоматически переносит тайтл в «Смотрю» / «Просмотрено» по локальному прогрессу просмотра.
 *
 * Запись local-first, как на экране деталей: сетевой сбой ставит мутацию в offline-очередь, иная
 * ошибка откатывает локальную запись. Handler не владеет корутинами — вызывается из ViewModel.
 */
@ViewModelScoped
internal class PlayerLibraryAutoListHandler @Inject constructor(
    private val settingsStore: PlayerSettingsStore,
    private val getAccountSession: GetAccountSessionUseCase,
    private val getAnimeWatchProgress: GetAnimeWatchProgressUseCase,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeListState: GetAnimeListStateUseCase,
    private val setAnimeList: SetAnimeListUseCase,
    private val upsertLibraryItem: UpsertLibraryItemUseCase,
    private val removeLibraryItem: RemoveLibraryItemUseCase,
    private val pendingMutationQueue: PendingMutationQueue,
) {
    private val appliedLists = mutableMapOf<Int, UserAnimeList>()

    /**
     * Проверяет прогресс тайтла после сохранения серии и при необходимости повышает список.
     * Дешёвые проверки идут первыми: чтение БД только когда серия уже просмотрена больше чем на 50%.
     */
    suspend fun onProgressSaved(animeId: Int, positionMs: Long, durationMs: Long) {
        // Побочное действие: сбой сети/БД не должен ломать сохранение прогресса в вызывающем коде.
        runSuspendCatching { updateList(animeId, positionMs, durationMs) }
    }

    private suspend fun updateList(animeId: Int, positionMs: Long, durationMs: Long) {
        if (animeId <= 0) return
        if (appliedLists[animeId] == UserAnimeList.COMPLETED) return
        if (progress(positionMs, durationMs) < AUTO_WATCHING_EPISODE_PROGRESS) return
        if (appliedLists[animeId] == UserAnimeList.WATCHING &&
            !isWatchedProgress(positionMs, durationMs)
        ) {
            return
        }
        if (!settingsStore.autoManageLibraryLists.first()) return
        val session = getAccountSession()
        if (!session.isAuthorized || session.userId <= 0) return

        val watchProgress = getAnimeWatchProgress(animeId)
        val details = getAnimeDetails(animeId)
        val target = autoLibraryTarget(
            progress = watchProgress,
            status = details.status,
            totalEpisodes = details.episodes?.count,
        ) ?: return
        if (appliedLists[animeId] == target) return

        val state = getAnimeListState(animeId)
        val previous = state?.list
        if (!previous.canAutoMoveTo(target)) {
            appliedLists[animeId] = previous ?: return
            return
        }

        val isFavorite = state?.isFavorite ?: false
        upsertLibraryItem(details.toLibraryItem(target, isFavorite))
        val result = runSuspendCatching { setAnimeList(animeId, target) }
        val error = result.exceptionOrNull()
        if (error == null || pendingMutationQueue.enqueueOnNetworkFailure(
                PendingMutation.SetList(animeId, target.id),
                error,
            )
        ) {
            appliedLists[animeId] = target
            return
        }
        if (previous != null) {
            upsertLibraryItem(details.toLibraryItem(previous, isFavorite))
        } else {
            removeLibraryItem(animeId)
        }
    }
}
