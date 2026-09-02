package su.afk.yummy.tv.feature.details.episodes.handler

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.core.error.api.isNetworkError
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.storage.outbox.MarkWatchedPayload
import su.afk.yummy.tv.core.storage.outbox.PendingMutationOutbox
import su.afk.yummy.tv.core.storage.outbox.PendingMutationSyncScheduler
import su.afk.yummy.tv.core.storage.outbox.PendingMutationTypes
import su.afk.yummy.tv.core.storage.outbox.RemoveWatchedPayload
import su.afk.yummy.tv.domain.account.usecase.RemoveWatchedVideosUseCase
import su.afk.yummy.tv.domain.account.usecase.SaveVideoWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.ClearEpisodeWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.MarkEpisodeWatchedLocallyUseCase
import javax.inject.Inject

/**
 * Отметка серии просмотренной без фактического воспроизведения — и снятие такой отметки.
 *
 * Локальное состояние пишется всегда, серверное — только под авторизованным аккаунтом. Отдельного
 * флага "просмотрено" нет ни локально, ни на сервере: локально это позиция, равная длительности,
 * на сервере — `PUT /video` с временем у конца серии.
 */
internal class EpisodeWatchedHandler @Inject constructor(
    private val markEpisodeWatchedLocally: MarkEpisodeWatchedLocallyUseCase,
    private val clearEpisodeWatchProgress: ClearEpisodeWatchProgressUseCase,
    private val saveVideoWatchProgress: SaveVideoWatchProgressUseCase,
    private val removeWatchedVideos: RemoveWatchedVideosUseCase,
    private val pendingMutationOutbox: PendingMutationOutbox,
    private val pendingMutationSyncScheduler: PendingMutationSyncScheduler,
) {

    /** Метаданные тайтла и серии для карточки Continue Watching. */
    data class EpisodeMeta(
        val animeTitle: String,
        val posterUrl: String,
        val screenshotUrl: String,
    )

    /**
     * Возвращает false, если серверная часть не удалась. Локальная отметка при этом уже проставлена:
     * расхождение подчистится ближайшим перезапросом списка серий.
     */
    suspend fun markWatched(
        animeId: Int,
        episode: String,
        videos: List<AnimeVideo>,
        bestDubbing: String,
        existing: AnimeWatchProgress?,
        meta: EpisodeMeta,
        isSignedIn: Boolean,
    ): Boolean {
        val target = videos.representativeVideo(bestDubbing, existing) ?: return true
        val durationMs = resolveDurationMs(existing, videos)

        markEpisodeWatchedLocally(
            animeId = animeId,
            episode = episode,
            videoId = target.id,
            episodeUrl = target.iframeUrl,
            durationMs = durationMs,
            animeTitle = meta.animeTitle,
            posterUrl = meta.posterUrl,
            playerName = target.player,
            dubbing = target.dubbing,
            screenshotUrl = meta.screenshotUrl,
        )

        if (!isSignedIn || target.id <= 0) return true
        val durationSeconds = (durationMs / 1_000L).toInt()
        val timeSeconds = (durationSeconds - WATCH_END_TOLERANCE_SECONDS).coerceAtLeast(0)
        return runCatchingMutation(
            onNetworkFailure = {
                pendingMutationOutbox.enqueue(
                    type = PendingMutationTypes.MARK_WATCHED,
                    payloadJson = MarkWatchedPayload(
                        target.id,
                        timeSeconds,
                        durationSeconds
                    ).encode(),
                )
            },
        ) {
            saveVideoWatchProgress(
                videoId = target.id,
                timeSeconds = timeSeconds,
                durationSeconds = durationSeconds,
                // Серия не просматривалась фактически, поэтому просмотренных секунд не добавляем:
                // сервер считает spent_time именно из times.
                times = emptyList(),
            )
        }
    }

    /**
     * Снимает отметку. На сервере чистятся отметки всех озвучек серии — иначе оставшаяся отметка
     * другой озвучки вернёт статус при слиянии локального и серверного прогресса.
     */
    suspend fun unmarkWatched(
        animeId: Int,
        episode: String,
        videos: List<AnimeVideo>,
        isSignedIn: Boolean,
    ): Boolean {
        clearEpisodeWatchProgress(animeId, episode)

        if (!isSignedIn) return true
        val videoIds = videos.map { it.id }.filter { it > 0 }
        if (videoIds.isEmpty()) return true
        return runCatchingMutation(
            onNetworkFailure = {
                pendingMutationOutbox.enqueue(
                    type = PendingMutationTypes.REMOVE_WATCHED,
                    payloadJson = RemoveWatchedPayload(videoIds).encode(),
                )
            },
        ) { removeWatchedVideos(videoIds) }
    }

    /**
     * Сетевые сбои ставятся в offline-очередь и считаются успехом — локальная отметка уже
     * применена, а [PendingMutationSyncScheduler] дошлёт мутацию, как только появится сеть.
     * Остальные ошибки (не связанные с сетью) по-прежнему возвращают false.
     */
    private suspend fun runCatchingMutation(
        onNetworkFailure: suspend () -> Unit,
        block: suspend () -> Boolean,
    ): Boolean =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            if (error.isNetworkError()) {
                onNetworkFailure()
                pendingMutationSyncScheduler.scheduleFlush()
                true
            } else {
                false
            }
        }

    /**
     * Длительность нужна обязательно: правило "просмотрено" не работает при нулевой длительности.
     * Берём известную из уже сохранённого прогресса, затем из данных серии, и лишь в крайнем
     * случае — типовую длительность эпизода.
     */
    private fun resolveDurationMs(
        existing: AnimeWatchProgress?,
        videos: List<AnimeVideo>,
    ): Long =
        existing?.durationMs?.takeIf { it > 0L }
            ?: videos.firstNotNullOfOrNull { video ->
                video.durationSeconds?.takeIf { it > 0 }?.times(1_000L)
            }
            ?: FALLBACK_EPISODE_DURATION_MS

    /**
     * Видео, к которому привязывается отметка: сначала то, по которому прогресс уже есть, затем
     * озвучка по умолчанию для тайтла, иначе первое доступное.
     */
    private fun List<AnimeVideo>.representativeVideo(
        bestDubbing: String,
        existing: AnimeWatchProgress?,
    ): AnimeVideo? =
        firstOrNull { existing != null && existing.videoId > 0 && it.id == existing.videoId }
            ?: firstOrNull { bestDubbing.isNotBlank() && it.dubbing == bestDubbing }
            ?: firstOrNull()

    private companion object {
        /** Тот же допуск до конца серии, что использует прогресс плеера. */
        const val WATCH_END_TOLERANCE_SECONDS = 10

        /** 24 минуты — типовая длительность серии, когда API её не сообщил. */
        const val FALLBACK_EPISODE_DURATION_MS = 24 * 60 * 1_000L
    }
}
