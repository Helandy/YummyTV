package su.afk.yummy.tv.core.model.anime

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.common.player.isServerMirrorOfExtraSource

/**
 * Серверный прогресс видео из `/videos` (`watched`), приведённый к [AnimeWatchProgress].
 * `null`, если сервер ничего не помнит про видео или не прислал длительность.
 */
fun AnimeVideo.toServerWatchProgress(animeId: Int): AnimeWatchProgress? =
    serverWatchProgressOf(
        animeId = animeId,
        episode = episode,
        videoId = id,
        episodeUrl = iframeUrl,
        watchedEndTimeSeconds = watchedEndTimeSeconds,
        watchedDateSeconds = watchedDateSeconds,
        durationSeconds = durationSeconds,
        playerName = player,
        dubbing = dubbing,
    )

/** То же для сырых полей видео — когда [AnimeVideo] под рукой нет (кэш в Room). */
fun serverWatchProgressOf(
    animeId: Int,
    episode: String,
    videoId: Int,
    episodeUrl: String,
    watchedEndTimeSeconds: Int?,
    watchedDateSeconds: Long?,
    durationSeconds: Int?,
    playerName: String,
    dubbing: String,
): AnimeWatchProgress? {
    val positionSeconds = watchedEndTimeSeconds?.takeIf { it >= 0 } ?: return null
    val dateSeconds = watchedDateSeconds?.takeIf { it > 0L } ?: return null
    val durationMs = durationSeconds?.takeIf { it > 0 }?.times(1_000L) ?: return null
    return AnimeWatchProgress(
        animeId = animeId,
        episode = episode,
        videoId = videoId,
        episodeUrl = episodeUrl,
        positionMs = positionSeconds * 1_000L,
        durationMs = durationMs,
        updatedAt = dateSeconds * 1_000L,
        playerName = playerName,
        dubbing = dubbing,
    )
}

/**
 * Единое правило слияния серверного и локального прогресса: по одной записи на серию, побеждает
 * более свежая (при равенстве — серверная, она идёт первой), кроме серверного «зеркала» локального
 * прогресса 4K. Им пользуются и список серий тайтла, и «Продолжить просмотр», чтобы не расходиться.
 */
fun mergeEpisodeProgress(
    serverEntries: List<AnimeWatchProgress>,
    localEntries: List<AnimeWatchProgress>,
): List<AnimeWatchProgress> {
    val result = linkedMapOf<String, AnimeWatchProgress>()
    (serverEntries + localEntries).forEach { entry ->
        val key = entry.episodeMergeKey()
        val current = result[key]
        result[key] = if (current == null) entry else newerOf(current, entry)
    }
    return result.values.toList()
}

private fun newerOf(a: AnimeWatchProgress, b: AnimeWatchProgress): AnimeWatchProgress {
    val newer = if (b.updatedAt > a.updatedAt) b else a
    val older = if (newer === b) a else b
    val isMirror = isServerMirrorOfExtraSource(
        localEpisodeUrl = older.episodeUrl,
        localPositionMs = older.positionMs,
        serverEpisodeUrl = newer.episodeUrl,
        serverPositionMs = newer.positionMs,
    )
    return if (isMirror) older.copy(updatedAt = newer.updatedAt) else newer
}

private fun AnimeWatchProgress.episodeMergeKey(): String =
    when {
        animeId > 0 && episode.isNotBlank() -> "$animeId:episode:${episode.episodeGroupKey()}"
        videoId > 0 -> "video:$videoId"
        episodeUrl.isNotBlank() -> "url:$episodeUrl"
        else -> "entry:${hashCode()}"
    }
