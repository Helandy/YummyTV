package su.afk.yummy.tv.source.anime4k.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kEpisodeResponseDto
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kStreamFileDto
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kStreamsResponseDto
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kTicketResponseDto
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kTitleResponseDto
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kLookup

class Anime4kMapperTest : BaseUnitTest() {

    @Test
    fun `request carries lookup fields as they are`() {
        val request = Anime4kMapper.toRequest(Anime4kLookup(1735, "Наруто", listOf("Naruto"), 2002, "tv", 220))

        assertEquals(1735, request.malId)
        assertEquals("Наруто", request.title)
        assertEquals(listOf("Naruto"), request.otherTitles)
        assertEquals(2002, request.year)
        assertEquals("tv", request.type)
        assertEquals(220, request.episodes)
    }

    @Test
    fun `unavailable or empty title is null`() {
        assertNull(Anime4kMapper.toTitle(1, Anime4kTitleResponseDto(available = false)))
        assertNull(Anime4kMapper.toTitle(1, Anime4kTitleResponseDto(available = true, episodes = emptyList())))
        assertNull(
            Anime4kMapper.toTitle(
                1,
                Anime4kTitleResponseDto(true, listOf(Anime4kEpisodeResponseDto("1", "1", dubbings = listOf(" ")))),
            ),
        )
    }

    @Test
    fun `title keeps episodes with dubbings only`() {
        val title = Anime4kMapper.toTitle(
            7,
            Anime4kTitleResponseDto(
                true,
                listOf(
                    Anime4kEpisodeResponseDto("1", "1", listOf("AlexFilm", "")),
                    Anime4kEpisodeResponseDto("2", "1", emptyList()),
                    Anime4kEpisodeResponseDto("", "1", listOf("X")),
                ),
            ),
        )

        assertEquals(7, title?.malId)
        assertEquals(1, title?.episodes?.size)
        assertEquals(listOf("AlexFilm"), title?.episodes?.single()?.dubbings)
    }

    @Test
    fun `ticket without value is rejected`() {
        val error = runCatching { Anime4kMapper.toTicket(Anime4kTicketResponseDto(ticket = "")) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("abc", Anime4kMapper.toTicket(Anime4kTicketResponseDto("abc", 20, 300)).value)
    }

    @Test
    fun `streams are sorted and empty answer is an error`() {
        val streams = Anime4kMapper.toStreams(
            Anime4kStreamsResponseDto(
                "https://example.org/",
                listOf(Anime4kStreamFileDto(1080, 3000, "u1"), Anime4kStreamFileDto(2160, 7500, "u2"), Anime4kStreamFileDto(720, 1, "")),
            ),
        )

        assertEquals(listOf(2160, 1080), streams.files.map { it.quality })
        assertTrue(runCatching { Anime4kMapper.toStreams(Anime4kStreamsResponseDto("r", emptyList())) }.isFailure)
    }
}
