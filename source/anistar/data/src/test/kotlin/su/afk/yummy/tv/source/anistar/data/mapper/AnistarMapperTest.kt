package su.afk.yummy.tv.source.anistar.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.source.anistar.data.dto.AnistarTitleResponseDto
import su.afk.yummy.tv.source.anistar.domain.model.AnistarEpisode
import su.afk.yummy.tv.source.anistar.domain.model.AnistarLookup
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle

class AnistarMapperTest {
    @Test
    fun `request trims dedups and caps other titles`() {
        val lookup = AnistarLookup(1, "Наруто", listOf(" Naruto ", "", "Naruto") + (1..30).map { "t$it" }, 2007, "tv", 220)

        val request = AnistarMapper.toRequest(lookup)

        assertEquals(20, request.otherTitles.size)
        assertEquals("Naruto", request.originalTitle)
        assertEquals("Naruto", request.otherTitles.first())
        assertEquals(2007, request.year)
    }

    @Test
    fun `title is built from an available response and the mirror is normalised`() {
        val title = AnistarMapper.toTitle(AnistarTitleResponseDto(true, 2658, "https://v31.astar.bz/", listOf(2, 1, 0)))

        assertEquals(AnistarTitle(2658, "https://v31.astar.bz", listOf(AnistarEpisode(2), AnistarEpisode(1))), title)
    }

    @Test
    fun `unavailable or incomplete responses give no title`() {
        assertNull(AnistarMapper.toTitle(AnistarTitleResponseDto(available = false, newsId = 1, mirror = "https://v31.astar.bz")))
        assertNull(AnistarMapper.toTitle(AnistarTitleResponseDto(true, null, "https://v31.astar.bz")))
        assertNull(AnistarMapper.toTitle(AnistarTitleResponseDto(true, 5, null)))
        assertNull(AnistarMapper.toTitle(AnistarTitleResponseDto(true, 5, "http://v31.astar.bz")))
    }
}
