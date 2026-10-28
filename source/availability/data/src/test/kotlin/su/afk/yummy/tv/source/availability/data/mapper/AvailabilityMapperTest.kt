package su.afk.yummy.tv.source.availability.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.source.availability.data.dto.AvailabilityResponseDto
import su.afk.yummy.tv.source.availability.data.dto.SourceAvailabilityDto
import su.afk.yummy.tv.source.availability.data.dto.SourceEpisodeDto
import su.afk.yummy.tv.source.availability.data.dto.SourceVariantDto
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup

class AvailabilityMapperTest {

    @Test
    fun `request puts the latin original first and drops duplicates`() {
        val lookup = AvailabilityLookup(
            malId = 5, title = "Фрирен", otherTitles = listOf("Фрирен", "Sousou no Frieren", "葬送のフリーレン"),
            year = 2023, type = "tv", episodes = 28,
        )

        val request = AvailabilityMapper.toRequest(lookup)

        assertEquals(listOf("Sousou no Frieren", "Фрирен", "葬送のフリーレン"), request.titles)
        assertEquals(5, request.malId)
        assertEquals(28, request.episodes)
    }

    @Test
    fun `answer drops entries without key or url and keeps the failed sources`() {
        val dto = AvailabilityResponseDto(
            sources = listOf(
                SourceAvailabilityDto("ailiberty", "k", "https://x/k", 3, 0, true, listOf("720p")),
                SourceAvailabilityDto("broken", "", "https://x/y"),
            ),
            failed = listOf("slow"),
        )

        val answer = AvailabilityMapper.toAnswer(dto)

        assertEquals(listOf("ailiberty"), answer.sources.map { it.sourceId })
        assertNull(answer.sources.single().episodesTotal)
        assertEquals(setOf("slow"), answer.failedSourceIds)
    }

    @Test
    fun `variants keep kind, order episodes by number and drop broken entries`() {
        val dto = AvailabilityResponseDto(
            sources = listOf(
                SourceAvailabilityDto(
                    sourceId = "anilibria", key = "1", url = "https://x/1", episodesAvailable = 2,
                    variants = listOf(
                        SourceVariantDto(
                            key = "1", kind = "voice",
                            episodes = listOf(
                                SourceEpisodeDto(2, 1500, 90, 180), SourceEpisodeDto(1, 0), SourceEpisodeDto(1, 1400), SourceEpisodeDto(0),
                            ),
                        ),
                        SourceVariantDto(key = "2", kind = "dubbing", episodes = listOf(SourceEpisodeDto(1))),
                        SourceVariantDto(key = "", kind = "voice"),
                    ),
                ),
            ),
        )

        val variants = AvailabilityMapper.toAnswer(dto).sources.single().variants

        assertEquals(listOf("1", "2"), variants.map { it.key })
        assertEquals(listOf(false, true), variants.map { it.isDubbing })
        assertEquals(listOf(1, 2), variants[0].episodes.map { it.number })
        assertNull(variants[0].episodes[0].durationSec)
        assertEquals(90, variants[0].episodes[1].openingStartSec)
    }

    @Test
    fun `episode id and url survive and blank ones become null`() {
        val dto = AvailabilityResponseDto(
            sources = listOf(
                SourceAvailabilityDto(
                    "amedia", "5-x", "https://amd.online/5-x.html", 2,
                    variants = listOf(
                        SourceVariantDto(
                            "5",
                            episodes = listOf(
                                SourceEpisodeDto(1, id = "47997"),
                                SourceEpisodeDto(2, url = "https://rutube.ru/play/embed/abc", id = " "),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val episodes = AvailabilityMapper.toAnswer(dto).sources.single().variants.single().episodes

        assertEquals("47997", episodes[0].id)
        assertNull(episodes[0].url)
        assertNull(episodes[1].id)
        assertEquals("https://rutube.ru/play/embed/abc", episodes[1].url)
    }

    @Test
    fun `variant title survives and a blank one becomes null`() {
        val dto = AvailabilityResponseDto(
            sources = listOf(
                SourceAvailabilityDto(
                    "aniboom", "0BzM07Pq71J", "https://animego.me/anime/x", 1,
                    variants = listOf(
                        SourceVariantDto("16", kind = "voice", title = "JAM CLUB", episodes = listOf(SourceEpisodeDto(1))),
                        SourceVariantDto("30", kind = "voice", title = " ", episodes = listOf(SourceEpisodeDto(1))),
                    ),
                ),
            ),
        )

        val variants = AvailabilityMapper.toAnswer(dto).sources.single().variants

        assertEquals("JAM CLUB", variants[0].title)
        assertNull(variants[1].title)
    }

    @Test
    fun `sub and subs kinds are subtitles, voice is not`() {
        val dto = AvailabilityResponseDto(
            sources = listOf(
                SourceAvailabilityDto(
                    "mikai", "1", "https://mikai.me/anime/1", 1,
                    variants = listOf(
                        SourceVariantDto("ashdi:1", kind = "voice", episodes = listOf(SourceEpisodeDto(1))),
                        SourceVariantDto("ashdi:2", kind = "subs", episodes = listOf(SourceEpisodeDto(1))),
                        SourceVariantDto("1:2", kind = "sub", episodes = listOf(SourceEpisodeDto(1))),
                    ),
                ),
            ),
        )

        val variants = AvailabilityMapper.toAnswer(dto).sources.single().variants

        assertEquals(listOf(false, true, true), variants.map { it.isSubs })
    }

    @Test
    fun `mirrors survive without blanks and repeats`() {
        val dto = AvailabilityResponseDto(
            sources = listOf(
                SourceAvailabilityDto(
                    "animebesst", "1-x", "https://anime1.best/anime1-online/1-x.html", 1,
                    mirrors = listOf("https://ab2.animebesst.org/anime1-online/1-x.html", " ", "https://ab2.animebesst.org/anime1-online/1-x.html"),
                ),
            ),
        )

        assertEquals(listOf("https://ab2.animebesst.org/anime1-online/1-x.html"), AvailabilityMapper.toAnswer(dto).sources.single().mirrors)
    }
}
