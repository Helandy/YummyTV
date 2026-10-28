package su.afk.yummy.tv.core.utils.episode

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.utils.kodik.normalizeIframeUrl

/**
 * Единая модель превью серии для Coil и для [EpisodeThumbnailResolver]: все экраны (серии, «Продолжить», история,
 * «Отложенные», загрузки) описывают серию одинаково, а откуда брать кадр, решает резолвер.
 *
 * [animeId] и [episode] опознают серию (по ним ищутся Tenrai и Kodik-видео тайтла); [iframeUrl] и [imageUrl] —
 * подсказки, которые у экрана уже есть: Kodik-iframe серии и готовая картинка. Ключ кэша общий для серии
 * ([episodeThumbnailCacheKey]): у одной серии на разных экранах разные подсказки (озвучка), а кадр должен быть один и
 * скачиваться один раз.
 */
data class EpisodeThumbnail(
    val animeId: Int = 0,
    val episode: String = "",
    val iframeUrl: String = "",
    val imageUrl: String = "",
) {
    val isEpisodeKnown: Boolean
        get() = animeId > 0 && episode.isNotBlank()

    /** Прежний ключ по iframe: по нему ещё лежат уже скачанные превью, читаем их с диска без сети. */
    val legacyCacheKey: String?
        get() = iframeUrl.takeIf { it.isNotBlank() }?.let { "kodik_thumb:" + normalizeIframeUrl(it) }

    val cacheKey: String
        get() = when {
            isEpisodeKnown -> episodeThumbnailCacheKey(animeId, episode)
            legacyCacheKey != null -> legacyCacheKey!!
            else -> "image_thumb:$imageUrl"
        }

    /** Ключи, под которыми кадр мог лечь раньше: сначала Tenrai-ключ прошлой версии, затем ключ по iframe. */
    val fallbackCacheKeys: List<String>
        get() = listOfNotNull(
            tenraiThumbnailCacheKey(animeId, episode).takeIf { isEpisodeKnown },
            legacyCacheKey,
        ).filter { it != cacheKey }

    /** Есть ли вообще за что зацепиться: известная серия, iframe или готовая картинка. */
    val isResolvable: Boolean
        get() = isEpisodeKnown || iframeUrl.isNotBlank() || imageUrl.isNotBlank()
}

/** Ключ превью серии, не зависящий от озвучки: `"01"` и `"1"` — одна серия. */
fun episodeThumbnailCacheKey(animeId: Int, episode: String): String =
    "episode_thumb:$animeId:${episode.episodeGroupKey()}"

/** Ключ, под которым Tenrai-кадры лежали на диске до единого резолвера; читаем его, чтобы не качать заново. */
fun tenraiThumbnailCacheKey(animeId: Int, episode: String): String =
    "tenrai_thumb:$animeId:${episode.episodeGroupKey()}"
