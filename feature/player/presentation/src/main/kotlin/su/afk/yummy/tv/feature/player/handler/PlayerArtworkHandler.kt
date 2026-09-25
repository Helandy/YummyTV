package su.afk.yummy.tv.feature.player.handler

import su.afk.yummy.tv.core.utils.kodik.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.core.utils.kodik.resolveContinueWatchingImage
import su.afk.yummy.tv.feature.player.model.PlayerArtworkSource
import javax.inject.Inject

internal class PlayerArtworkHandler @Inject constructor(
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
) {
    /** Обложка серии для медиа-уведомления; если превью не получилось — постер тайтла. */
    suspend fun resolve(source: PlayerArtworkSource): String? {
        val poster = source.posterUrl.takeIf(String::isNotBlank)
        return resolveContinueWatchingImage(
            screenshotUrl = source.screenshotUrl,
            episodeUrl = source.episodeUrl,
            posterUrl = source.posterUrl,
            resolveKodikThumbnail = { resolveKodikThumbnailUrl(it) },
        ) ?: poster
    }
}
