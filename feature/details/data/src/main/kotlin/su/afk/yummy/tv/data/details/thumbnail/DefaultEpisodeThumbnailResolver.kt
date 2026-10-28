package su.afk.yummy.tv.data.details.thumbnail

import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnail
import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnailResolver
import su.afk.yummy.tv.core.utils.kodik.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.core.utils.kodik.kodikThumbnailIframeUrl
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Источники превью серии по порядку: Tenrai (кадры MyAnimeList), готовая картинка, которая уже есть у экрана, затем Kodik
 * (iframe экрана или, если его нет, Kodik-видео серии из уже закэшированного списка видео тайтла — сеть за списком
 * не ходит). Kodik остаётся запасным: он нужен, когда Tenrai кадра не знает (свежие и многие старые тайтлы).
 */
@Singleton
class DefaultEpisodeThumbnailResolver @Inject constructor(
    private val tenraiThumbnails: TenraiEpisodeThumbnails,
    private val getCachedAnimeVideos: GetCachedAnimeVideosUseCase,
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
) : EpisodeThumbnailResolver {

    override suspend fun resolveUrl(thumbnail: EpisodeThumbnail): String? {
        if (thumbnail.isEpisodeKnown) {
            tenraiThumbnails.previewUrl(thumbnail.animeId, thumbnail.episode)?.let { return it }
        }
        thumbnail.imageUrl.takeIf { it.isNotBlank() }?.let { return it }
        val iframeUrl = thumbnail.iframeUrl.takeIf { it.isNotBlank() }
            ?: cachedKodikIframeUrl(thumbnail)
            ?: return null
        return resolveKodikThumbnailUrl(iframeUrl)
    }

    private suspend fun cachedKodikIframeUrl(thumbnail: EpisodeThumbnail): String? {
        if (!thumbnail.isEpisodeKnown) return null
        val episodeKey = thumbnail.episode.episodeGroupKey()
        return runSuspendCatching { getCachedAnimeVideos(thumbnail.animeId) }
            .getOrNull()
            .orEmpty()
            .filter { it.episode.episodeGroupKey() == episodeKey }
            .kodikThumbnailIframeUrl()
    }
}
