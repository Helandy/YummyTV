package su.afk.yummy.tv.source.availability.data.mapper

import su.afk.yummy.tv.core.model.anime.utils.originalAnimeTitle
import su.afk.yummy.tv.core.model.anime.utils.prioritizeAnimeTitles
import su.afk.yummy.tv.source.availability.data.dto.AvailabilityRequestDto
import su.afk.yummy.tv.source.availability.data.dto.AvailabilityResponseDto
import su.afk.yummy.tv.source.availability.data.dto.SourceVariantDto
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityAnswer
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode
import su.afk.yummy.tv.source.availability.domain.model.SourceVariant

internal object AvailabilityMapper {
    private const val MAX_OTHER_TITLES = 25

    /** Название на латинице идёт первым (сервер сверяет его с оригиналом на сайте), за ним основное и остальные. */
    fun toRequest(lookup: AvailabilityLookup): AvailabilityRequestDto = AvailabilityRequestDto(
        titles = (
            listOfNotNull(originalAnimeTitle(lookup.otherTitles)) + lookup.title +
                prioritizeAnimeTitles(lookup.title, lookup.otherTitles, MAX_OTHER_TITLES)
            ).distinct(),
        malId = lookup.malId,
        year = lookup.year,
        type = lookup.type,
        episodes = lookup.episodes,
    )

    /** Записи без ключа или адреса отбрасываются: по ним нечего открывать. */
    fun toAnswer(dto: AvailabilityResponseDto): AvailabilityAnswer = AvailabilityAnswer(
        sources = dto.sources
            .filter { it.sourceId.isNotBlank() && it.key.isNotBlank() && it.url.isNotBlank() }
            .map {
                SourceAvailability(
                    sourceId = it.sourceId,
                    key = it.key,
                    url = it.url,
                    episodesAvailable = (it.episodesAvailable ?: 0).coerceAtLeast(0),
                    episodesTotal = it.episodesTotal?.takeIf { total -> total > 0 },
                    ongoing = it.ongoing,
                    qualities = it.qualities,
                    variants = it.variants.mapNotNull(::toVariant),
                    mirrors = it.mirrors.map(String::trim).filter(String::isNotEmpty).distinct(),
                )
            },
        failedSourceIds = dto.failed.toSet(),
    )

    /** Вариант без ключа отбрасывается; серии с неположительным номером и повторы номеров — тоже, порядок по номеру. */
    private fun toVariant(dto: SourceVariantDto): SourceVariant? {
        if (dto.key.isBlank()) return null
        return SourceVariant(
            key = dto.key,
            isDubbing = dto.kind == KIND_DUBBING,
            isSubs = dto.kind in KINDS_SUBS,
            title = dto.title?.takeIf { it.isNotBlank() },
            episodes = dto.episodes.filter { it.number > 0 }.distinctBy { it.number }.sortedBy { it.number }.map {
                SourceEpisode(
                    number = it.number,
                    durationSec = it.durationSec?.takeIf { sec -> sec > 0 },
                    openingStartSec = it.openingStartSec,
                    openingEndSec = it.openingEndSec,
                    id = it.id?.takeIf { value -> value.isNotBlank() },
                    url = it.url?.takeIf { value -> value.isNotBlank() },
                )
            },
        )
    }

    private const val KIND_DUBBING = "dubbing"

    /** Субтитры вместо озвучки: AnimeON отдаёт `sub`, Mikai — `subs`. */
    private val KINDS_SUBS = setOf("sub", "subs")
}
