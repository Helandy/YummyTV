package su.afk.yummy.tv.source.aniboom.data.mapper

import su.afk.yummy.tv.core.model.anime.utils.originalAnimeTitle
import su.afk.yummy.tv.core.model.anime.utils.prioritizeAnimeTitles
import su.afk.yummy.tv.source.aniboom.data.dto.AniboomLookupRequestDto
import su.afk.yummy.tv.source.aniboom.data.dto.AniboomTitleResponseDto
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomDubbing
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomLookup
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTitle

internal object AniboomMapper {
    private const val MAX_OTHER_TITLES = 20

    fun toRequest(lookup: AniboomLookup): AniboomLookupRequestDto = AniboomLookupRequestDto(
        malId = lookup.malId,
        title = lookup.title,
        originalTitle = originalAnimeTitle(lookup.otherTitles),
        otherTitles = prioritizeAnimeTitles(lookup.title, lookup.otherTitles, MAX_OTHER_TITLES),
        year = lookup.year,
        type = lookup.type,
        episodes = lookup.episodes,
    )

    /** `null`, если сервер не нашёл тайтл или ответ не годится для воспроизведения (нет id или озвучек). */
    fun toTitle(dto: AniboomTitleResponseDto): AniboomTitle? {
        val aniboomId = dto.aniboomId?.takeIf { dto.available && it.isNotBlank() } ?: return null
        val dubbings = dto.dubbings
            .filter { it.translation > 0 && it.title.isNotBlank() }
            .distinctBy { it.translation }
            .map { AniboomDubbing(it.title, it.translation) }
        if (dubbings.isEmpty()) return null
        return AniboomTitle(aniboomId, dto.episodesAvailable.coerceAtLeast(1), dubbings)
    }
}
