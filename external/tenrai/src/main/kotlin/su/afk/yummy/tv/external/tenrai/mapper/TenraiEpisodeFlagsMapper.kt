package su.afk.yummy.tv.external.tenrai.mapper

import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.external.tenrai.dto.TenraiEpisodeDto

/** Ключ — номер серии строкой; серии без пометок отбрасываются. */
internal fun List<TenraiEpisodeDto>.toEpisodeFlagsByNumber(): Map<String, EpisodeFlags> =
    asSequence()
        .filter { it.episodeNumber > 0 && (it.filler || it.recap) }
        .associate { it.episodeNumber.toString() to EpisodeFlags(filler = it.filler, recap = it.recap) }
