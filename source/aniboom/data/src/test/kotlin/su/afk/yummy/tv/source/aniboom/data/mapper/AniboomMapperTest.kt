package su.afk.yummy.tv.source.aniboom.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.aniboom.data.dto.AniboomDubbingResponseDto
import su.afk.yummy.tv.source.aniboom.data.dto.AniboomTitleResponseDto
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomDubbing
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomLookup

class AniboomMapperTest : BaseUnitTest() {

    @Test
    fun `request trims dedups and limits other titles`() {
        val lookup = AniboomLookup(1, "Фрирен", listOf(" Frieren ", "Frieren", "", "Sousou no Frieren") + List(30) { "t$it" }, 2023, "tv", 28)

        val request = AniboomMapper.toRequest(lookup)

        assertEquals(20, request.otherTitles.size)
        assertEquals(listOf("Frieren", "Sousou no Frieren"), request.otherTitles.take(2))
        assertEquals(28, request.episodes)
    }

    @Test
    fun `available response becomes a title with at least one episode`() {
        val dto = AniboomTitleResponseDto(
            available = true, aniboomId = "0BzM07Pq71J", episodesAvailable = 0,
            dubbings = listOf(
                AniboomDubbingResponseDto("JAM CLUB", 16),
                AniboomDubbingResponseDto("JAM CLUB", 16),
                AniboomDubbingResponseDto("", 5),
                AniboomDubbingResponseDto("Bad", 0),
            ),
        )

        val title = AniboomMapper.toTitle(dto)

        assertEquals("0BzM07Pq71J", title?.aniboomId)
        assertEquals(1, title?.episodesAvailable)
        assertEquals(listOf(AniboomDubbing("JAM CLUB", 16)), title?.dubbings)
    }

    @Test
    fun `unavailable or unusable response is null`() {
        assertNull(AniboomMapper.toTitle(AniboomTitleResponseDto(available = false)))
        assertNull(AniboomMapper.toTitle(AniboomTitleResponseDto(available = true, aniboomId = null)))
        assertNull(AniboomMapper.toTitle(AniboomTitleResponseDto(available = true, aniboomId = "0BzM07Pq71J")))
    }
}
