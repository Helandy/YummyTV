package su.afk.yummy.tv.feature.player.handler

import su.afk.yummy.tv.core.model.anime.EpisodePreviewProvider
import su.afk.yummy.tv.core.utils.kodik.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.core.utils.kodik.resolveContinueWatchingImage
import su.afk.yummy.tv.feature.player.model.PlayerArtworkSource
import javax.inject.Inject

internal class PlayerArtworkHandler @Inject constructor(
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
    private val episodePreviews: EpisodePreviewProvider,
) {
    /** Обложка серии для медиа-уведомления; если превью не получилось — постер тайтла. */
    suspend fun resolve(source: PlayerArtworkSource): String? {
        val poster = source.posterUrl.takeIf(String::isNotBlank)
        episodePreviews.previewUrl(source.animeId, source.episode)?.let { return it }
        return resolveContinueWatchingImage(
            screenshotUrl = source.screenshotUrl,
            episodeUrl = source.episodeUrl,
            // Пустой постер — «постера нет»: иначе резолвер вернул бы "" вместо null.
            posterUrl = poster,
            resolveKodikThumbnail = { resolveKodikThumbnailUrl(it) },
        ) ?: poster
    }
}
