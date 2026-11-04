package su.afk.yummy.tv.data.details.mapper

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.model.anime.AnimeTmdbImages
import su.afk.yummy.tv.data.details.dto.YummyEpisodeDto
import su.afk.yummy.tv.data.details.dto.YummyEpisodesDto
import su.afk.yummy.tv.data.details.utils.sizedUrl

private const val STILL_SIZE = "w1280"

/** Кадры TMDB из ответа YummyTV API. Серия 1 — кадры `stills` (w1280); превью серий оставляем в размере, как пришли. */
internal fun YummyEpisodesDto.toAnimeTmdbImages(): AnimeTmdbImages = AnimeTmdbImages(
    firstEpisodeStillUrls = episodes.firstOrNull { it.episodeNumber == 1 }
        ?.stills.orEmpty()
        .mapNotNull { it.sizedUrl(STILL_SIZE) }
        .distinct(),
    episodePreviewUrls = episodeUrls { it.previewUrl },
    episodeMirrorUrls = episodeUrls { it.previewMirrorUrl },
)

private fun YummyEpisodesDto.episodeUrls(select: (YummyEpisodeDto) -> String?): Map<String, String> =
    episodes.asSequence()
        .filter { it.episodeNumber > 0 }
        .mapNotNull { episode ->
            select(episode)?.takeIf { it.isNotBlank() }?.let { episode.episodeNumber.toString().episodeGroupKey() to it }
        }
        .toMap()
