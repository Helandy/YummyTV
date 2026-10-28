package su.afk.yummy.tv.core.utils.episode

import su.afk.yummy.tv.core.utils.kodik.isKodikIframeUrl
import su.afk.yummy.tv.core.utils.network.isLikelyImageUrl

/**
 * Превью серии из того, что хранит запись просмотра («Продолжить», история, «Отложенные»): Kodik-iframe лежит
 * в [screenshotUrl] или [episodeUrl], готовая картинка — в [screenshotUrl].
 */
fun episodeThumbnailOf(
    animeId: Int,
    episode: String,
    screenshotUrl: String = "",
    episodeUrl: String = "",
): EpisodeThumbnail = EpisodeThumbnail(
    animeId = animeId,
    episode = episode,
    iframeUrl = listOf(screenshotUrl, episodeUrl).firstOrNull { it.isKodikIframeUrl() }.orEmpty(),
    imageUrl = screenshotUrl.takeIf { it.isLikelyImageUrl() }.orEmpty(),
)

/**
 * Цепочка моделей картинки записи просмотра по убыванию предпочтения: превью серии, если за него есть за что
 * зацепиться, затем все версии постера из [posterUrls]. Если первая модель не загрузилась,
 * [su.afk.yummy.tv.core.designsystem.components.FallbackAsyncImage] берёт следующую.
 */
fun episodeThumbnailModels(
    thumbnail: EpisodeThumbnail,
    posterUrls: List<String> = emptyList(),
): List<Any> = listOfNotNull<Any>(thumbnail.takeIf { it.isResolvable }) + posterUrls

/** Ссылка на обложку серии для мест без Coil (Watch Next, медиа-уведомление): кадр серии, иначе [posterUrl]. */
suspend fun EpisodeThumbnailResolver.resolveArtworkUrl(thumbnail: EpisodeThumbnail, posterUrl: String?): String? =
    (thumbnail.takeIf { it.isResolvable }?.let { resolveUrl(it) }) ?: posterUrl
