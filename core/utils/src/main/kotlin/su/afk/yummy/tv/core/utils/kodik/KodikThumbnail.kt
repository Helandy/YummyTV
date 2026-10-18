package su.afk.yummy.tv.core.utils.kodik

import su.afk.yummy.tv.core.common.episode.episodeGroupKey

/**
 * Модель для Coil: превью серии по kodik-iframe.
 *
 * Если известны [animeId] и [episode], ключ кэша — общий для серии ([episodeThumbnailCacheKey]):
 * у одной серии на разных экранах разные iframe (просмотренная озвучка, лучшая, самая популярная),
 * а превью должно быть одно и скачиваться один раз. [legacyCacheKey] — прежний ключ по iframe:
 * по нему ещё лежат уже скачанные превью, их читаем с диска без сети.
 */
data class KodikThumbnail(
    val iframeUrl: String,
    val animeId: Int = 0,
    val episode: String = "",
) {
    val legacyCacheKey: String
        get() = "kodik_thumb:" + normalizeIframeUrl(iframeUrl)

    val cacheKey: String
        get() = if (animeId > 0 && episode.isNotBlank()) {
            episodeThumbnailCacheKey(animeId, episode)
        } else {
            legacyCacheKey
        }
}

/** Ключ превью серии, не зависящий от озвучки: `"01"` и `"1"` — одна серия. */
fun episodeThumbnailCacheKey(animeId: Int, episode: String): String =
    "episode_thumb:$animeId:${episode.episodeGroupKey()}"
