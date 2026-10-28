package su.afk.yummy.tv.data.details.mapper

import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.data.details.dto.MalEpisodesDto

/** Ключ — номер серии строкой; серии без пометок отбрасываются. */
internal fun MalEpisodesDto.toEpisodeFlagsByNumber(): Map<String, EpisodeFlags> =
    data.asSequence()
        .filter { it.episodeNumber > 0 && (it.filler || it.recap) }
        .associate { it.episodeNumber.toString() to EpisodeFlags(filler = it.filler, recap = it.recap) }
