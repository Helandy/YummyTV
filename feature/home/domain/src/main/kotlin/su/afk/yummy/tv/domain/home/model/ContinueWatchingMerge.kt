package su.afk.yummy.tv.domain.home.model

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.common.episode.episodeNumberOrNull
import su.afk.yummy.tv.core.model.anime.isMeaningfulProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import kotlin.math.abs
import kotlin.math.floor

// Плеер шлёт на сервер позицию, зажатую в [0, duration − 10 с].
private const val ECHO_POSITION_TOLERANCE_MS = 15_000L

/**
 * Сливает локальный «Продолжить просмотр» с серверной историей: одна карточка на тайтл, побеждает
 * более свежая запись (при равенстве — локальная), итог отсортирован по дате.
 *
 * Серверная запись, совпавшая с локальной из [localHistory], — эхо нашей же выгрузки (после входа
 * сервер ставит ей время синхронизации), поэтому берёт локальную дату.
 * Серверная запись, удалённая вручную не позже своей даты ([suppressedAt]), игнорируется.
 * Досмотренная серверная серия не перебивает локальную цель «следующая серия». Если она свежее
 * локальной и не раньше её по номеру, то, как и плеер после watched-порога, предлагаем следующую
 * серию с начала при [suggestNextEpisode] (карточка [ContinueWatchingSource.REMOTE_NEXT_EPISODE]),
 * иначе скрываем тайтл.
 */
fun List<HomeContinueWatchingItem>.mergeWithRemote(
    remote: List<HomeContinueWatchingItem>,
    localHistory: List<HomeContinueWatchingItem>,
    suppressedAt: Map<Int, Long>,
    suggestNextEpisode: Boolean,
): List<HomeContinueWatchingItem> {
    val localByAnime = filter { it.animeId > 0 }
        .groupBy { it.animeId }
        .mapValues { (_, items) -> items.maxBy { it.updatedAt } }
    val historyByEpisode = localHistory.groupBy { it.animeId to it.episode.episodeGroupKey() }
    val remoteByAnime = remote
        .map { it.withEchoDate(historyByEpisode) }
        .filter { it.animeId > 0 && it.updatedAt > (suppressedAt[it.animeId] ?: Long.MIN_VALUE) }
        .groupBy { it.animeId }
        .mapValues { (_, items) -> items.maxBy { it.updatedAt } }

    return (localByAnime.keys + remoteByAnime.keys)
        .mapNotNull { animeId ->
            pickCard(
                local = localByAnime[animeId],
                remote = remoteByAnime[animeId],
                suggestNextEpisode = suggestNextEpisode,
            )
        }
        .sortedByDescending { it.updatedAt }
}

private fun HomeContinueWatchingItem.withEchoDate(
    historyByEpisode: Map<Pair<Int, String>, List<HomeContinueWatchingItem>>,
): HomeContinueWatchingItem {
    val echoOf = historyByEpisode[animeId to episode.episodeGroupKey()]
        ?.filter { abs(it.positionMs - positionMs) <= ECHO_POSITION_TOLERANCE_MS }
        ?.minOfOrNull { it.updatedAt }
        ?: return this
    return if (echoOf < updatedAt) copy(updatedAt = echoOf) else this
}

private fun pickCard(
    local: HomeContinueWatchingItem?,
    remote: HomeContinueWatchingItem?,
    suggestNextEpisode: Boolean,
): HomeContinueWatchingItem? {
    if (remote == null || remote.updatedAt <= (local?.updatedAt ?: Long.MIN_VALUE)) return local
    if (remote.isResumable()) return remote
    if (!remote.isWatched()) return local
    if (local != null && !remote.isNotBeforeEpisodeOf(local)) return local
    return if (suggestNextEpisode) remote.toNextEpisodeCard() else null
}

/** Карточка следующей серии с начала; номер не распознан — предлагать нечего, тайтл скрыт. */
private fun HomeContinueWatchingItem.toNextEpisodeCard(): HomeContinueWatchingItem? {
    val nextEpisode = episode.episodeNumberOrNull()
        ?.let { floor(it).toLong() + 1 }
        ?: return null
    return copy(
        episode = nextEpisode.toString(),
        positionMs = 0L,
        durationMs = 0L,
        screenshotUrl = "",
        source = ContinueWatchingSource.REMOTE_NEXT_EPISODE,
    )
}

private fun HomeContinueWatchingItem.isResumable(): Boolean =
    isMeaningfulProgress(positionMs, durationMs) && !isWatchedProgress(positionMs, durationMs)

private fun HomeContinueWatchingItem.isWatched(): Boolean =
    isWatchedProgress(positionMs, durationMs)

private fun HomeContinueWatchingItem.isNotBeforeEpisodeOf(other: HomeContinueWatchingItem): Boolean {
    val episode = episode.episodeNumberOrNull() ?: return false
    val otherEpisode = other.episode.episodeNumberOrNull() ?: return false
    return episode >= otherEpisode
}
