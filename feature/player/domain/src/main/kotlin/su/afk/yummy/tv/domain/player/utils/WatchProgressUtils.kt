package su.afk.yummy.tv.domain.player.utils

import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository

/**
 * Время активности для новой записи прогресса серии: не раньше текущего момента и строго позже
 * уже сохранённой записи серии.
 */
internal suspend fun nextActivityUpdatedAt(
    repository: WatchProgressRepository,
    animeId: Int,
    episode: String,
): Long {
    val now = System.currentTimeMillis()
    if (animeId <= 0) return now
    val existingUpdatedAt = episode
        .takeIf { it.isNotBlank() }
        ?.let { repository.get(animeId, it)?.updatedAt }
        ?: 0L
    return maxOf(now, existingUpdatedAt + 1L)
}
