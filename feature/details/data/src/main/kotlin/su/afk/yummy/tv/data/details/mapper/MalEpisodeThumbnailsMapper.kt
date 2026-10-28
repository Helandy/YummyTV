package su.afk.yummy.tv.data.details.mapper

import su.afk.yummy.tv.data.details.dto.MalEpisodesDto

/** Ключ — номер серии строкой; серии без картинки отбрасываются. */
internal fun MalEpisodesDto.toThumbnailUrlByNumber(): Map<String, String> =
    data.asSequence()
        .filter { it.episodeNumber > 0 }
        .mapNotNull { episode ->
            val url = episode.images.jpg.imageUrl?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            episode.episodeNumber.toString() to url
        }
        .toMap()
