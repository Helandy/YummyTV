package su.afk.yummy.tv.source.anistar.data.mapper

import su.afk.yummy.tv.core.model.anime.utils.originalAnimeTitle
import su.afk.yummy.tv.core.model.anime.utils.prioritizeAnimeTitles
import su.afk.yummy.tv.source.anistar.data.dto.AnistarLookupRequestDto
import su.afk.yummy.tv.source.anistar.data.dto.AnistarTitleResponseDto
import su.afk.yummy.tv.source.anistar.domain.model.AnistarLookup
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle

internal object AnistarMapper {
    private const val MAX_OTHER_TITLES = 20

    fun toRequest(lookup: AnistarLookup): AnistarLookupRequestDto = AnistarLookupRequestDto(
        malId = lookup.malId,
        title = lookup.title,
        originalTitle = originalAnimeTitle(lookup.otherTitles),
        otherTitles = prioritizeAnimeTitles(lookup.title, lookup.otherTitles, MAX_OTHER_TITLES),
        year = lookup.year,
        type = lookup.type,
        episodes = lookup.episodes,
    )

    /** `null`, если сервер не нашёл тайтл или ответ не годится для воспроизведения (нет id или зеркала). */
    fun toTitle(dto: AnistarTitleResponseDto): AnistarTitle? {
        if (!dto.available) return null
        val newsId = dto.newsId?.takeIf { it > 0 } ?: return null
        val mirror = dto.mirror?.trim()?.trimEnd('/')?.takeIf { it.startsWith("https://") } ?: return null
        return AnistarTitle(newsId, mirror)
    }
}
