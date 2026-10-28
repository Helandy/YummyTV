package su.afk.yummy.tv.feature.player.handler

import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnailResolver
import su.afk.yummy.tv.core.utils.episode.episodeThumbnailOf
import su.afk.yummy.tv.core.utils.episode.resolveArtworkUrl
import su.afk.yummy.tv.feature.player.model.PlayerArtworkSource
import javax.inject.Inject

internal class PlayerArtworkHandler @Inject constructor(
    private val thumbnailResolver: EpisodeThumbnailResolver,
) {
    /** Обложка серии для медиа-уведомления; если превью не получилось — постер тайтла. */
    suspend fun resolve(source: PlayerArtworkSource): String? =
        thumbnailResolver.resolveArtworkUrl(
            thumbnail = episodeThumbnailOf(source.animeId, source.episode, source.screenshotUrl, source.episodeUrl),
            // Пустой постер — «постера нет»: иначе обложкой стала бы пустая строка.
            posterUrl = source.posterUrl.takeIf(String::isNotBlank),
        )
}
