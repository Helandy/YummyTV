package su.afk.yummy.tv.source.anilibria.data.mapper

import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaEpisodeDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaReleaseDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaSkipDto
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaEpisode
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaSkip

internal object AniLibriaMapper {

    /** Релиз нового дубляжа называется «… — новый дубляж». */
    private const val DUBBING_MARK = "дубляж"

    /** Серии с дробным или пустым номером (спешлы) отбрасываем: адрес серии строится по целому номеру. */
    fun toRelease(malId: Int, dto: AniLibriaReleaseDto): AniLibriaRelease =
        AniLibriaRelease(
            id = dto.id,
            malId = malId,
            episodes = dto.episodes.mapNotNull(::toEpisode).distinctBy { it.ordinal }.sortedBy { it.ordinal },
            isDubbing = dto.name?.main?.contains(DUBBING_MARK, ignoreCase = true) == true,
        )

    private fun toEpisode(dto: AniLibriaEpisodeDto): AniLibriaEpisode? {
        val ordinal = dto.ordinal?.takeIf { it > 0 && it % 1.0 == 0.0 }?.toInt() ?: return null
        val episode = AniLibriaEpisode(
            ordinal = ordinal,
            name = dto.name?.takeIf { it.isNotBlank() },
            hls480 = dto.hls_480?.takeIf { it.isNotBlank() },
            hls720 = dto.hls_720?.takeIf { it.isNotBlank() },
            hls1080 = dto.hls_1080?.takeIf { it.isNotBlank() },
            durationSec = dto.duration?.takeIf { it > 0 },
            opening = dto.opening?.let(::toSkip),
        )
        return episode.takeIf { it.hls480 != null || it.hls720 != null || it.hls1080 != null }
    }

    private fun toSkip(dto: AniLibriaSkipDto): AniLibriaSkip? {
        val start = dto.start?.toInt() ?: return null
        val stop = dto.stop?.toInt() ?: return null
        return AniLibriaSkip(start, stop).takeIf { start >= 0 && stop > start }
    }
}
