package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.core.common.anime.isReleasedAnimeStatus
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.core.model.anime.progress
import su.afk.yummy.tv.domain.account.model.UserAnimeList

/** Доля серии, после которой она засчитывается для перехода тайтла в «Смотрю». */
internal const val AUTO_WATCHING_EPISODE_PROGRESS = 0.5f

/** Сколько серий нужно посмотреть дольше [AUTO_WATCHING_EPISODE_PROGRESS], чтобы попасть в «Смотрю». */
internal const val AUTO_WATCHING_MIN_EPISODES = 2

/**
 * Список, в который тайтл нужно перенести по прогрессу, или `null`, если рано.
 *
 * Прогресс схлопывается по [episodeGroupKey]: одна серия в разных озвучках считается один раз.
 * «Просмотрено» — только для вышедшего тайтла с известным числом серий, когда досмотрены все;
 * «Смотрю» — когда [AUTO_WATCHING_MIN_EPISODES] серий просмотрены больше чем на 50%.
 */
internal fun autoLibraryTarget(
    progress: List<AnimeWatchProgress>,
    status: String?,
    totalEpisodes: Int?,
): UserAnimeList? {
    val byEpisode = progress
        .filter { it.episode.isNotBlank() }
        .groupBy { it.episode.episodeGroupKey() }
    val watchedCount = byEpisode.count { (_, list) -> list.any { it.isWatchedProgress() } }
    if (status.isReleasedAnimeStatus() && totalEpisodes != null && totalEpisodes > 0 &&
        watchedCount >= totalEpisodes
    ) {
        return UserAnimeList.COMPLETED
    }
    val halfWatchedCount = byEpisode.count { (_, list) ->
        list.any { it.progress() >= AUTO_WATCHING_EPISODE_PROGRESS }
    }
    return UserAnimeList.WATCHING.takeIf { halfWatchedCount >= AUTO_WATCHING_MIN_EPISODES }
}

/**
 * Автоматика только повышает список: из «нет списка»/«Запланировано» в «Смотрю» или «Просмотрено»
 * и из «Смотрю» в «Просмотрено». Остальное — осознанный выбор пользователя.
 */
internal fun UserAnimeList?.canAutoMoveTo(target: UserAnimeList): Boolean =
    when (this) {
        null, UserAnimeList.PLANNED -> true
        UserAnimeList.WATCHING -> target == UserAnimeList.COMPLETED
        UserAnimeList.COMPLETED, UserAnimeList.DROPPED, UserAnimeList.POSTPONED -> false
    }
