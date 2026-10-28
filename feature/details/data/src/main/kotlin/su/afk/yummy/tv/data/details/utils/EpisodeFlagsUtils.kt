package su.afk.yummy.tv.data.details.utils

import su.afk.yummy.tv.core.model.anime.AnimeEpisodes

/**
 * Тайтл вышел целиком: известно число серий и все они уже вышли, новых не ожидается. У такого тайтла пометки филлеров
 * почти не меняются, поэтому кэшируем их надолго; у идущего (и у тайтла с неизвестным числом серий) — ненадолго.
 */
internal fun AnimeEpisodes?.isFullyAired(): Boolean {
    val total = this?.count ?: return false
    val aired = this.aired ?: return false
    return total > 0 && aired >= total && (nextDateEpochSeconds ?: 0L) <= 0L
}
