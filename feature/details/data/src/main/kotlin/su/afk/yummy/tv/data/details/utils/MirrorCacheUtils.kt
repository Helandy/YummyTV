package su.afk.yummy.tv.data.details.utils

import su.afk.yummy.tv.core.storage.anime.AnimeDetailsCache
import su.afk.yummy.tv.core.storage.anime.AnimeVideosCache

/**
 * Минимальная карточка из резервной копии YummyTV (полных деталей там ещё нет): ни описания, ни жанров. Заменять ею полную
 * сохранённую карточку нельзя.
 */
internal fun AnimeDetailsCache.isMinimal(): Boolean = entry.description.isBlank() && genres.isEmpty()

/**
 * Видео из резервной копии не несут пользовательских полей (серверный прогресс, подписка), а у части тайтлов нет отметок пропуска и
 * длительности. Для тех же видео ([AnimeVideosCache.videos] по `videoId`) недостающее берётся из сохранённого списка [stored], чтобы
 * ответ копии не затирал то, что приложение уже знает.
 */
internal fun AnimeVideosCache.withUserStateFrom(stored: AnimeVideosCache?): AnimeVideosCache {
    if (stored == null || stored.videos.isEmpty()) return this
    val old = stored.videos.associateBy { it.videoId }
    return copy(
        videos = videos.map { video ->
            val previous = old[video.videoId] ?: return@map video
            video.copy(
                watchedEndTimeSeconds = video.watchedEndTimeSeconds ?: previous.watchedEndTimeSeconds,
                watchedDateSeconds = video.watchedDateSeconds ?: previous.watchedDateSeconds,
                subscribed = video.subscribed || previous.subscribed,
                durationSeconds = video.durationSeconds ?: previous.durationSeconds,
                openingStartMs = video.openingStartMs ?: previous.openingStartMs,
                openingEndMs = video.openingEndMs ?: previous.openingEndMs,
                endingStartMs = video.endingStartMs ?: previous.endingStartMs,
                endingEndMs = video.endingEndMs ?: previous.endingEndMs,
            )
        },
    )
}
