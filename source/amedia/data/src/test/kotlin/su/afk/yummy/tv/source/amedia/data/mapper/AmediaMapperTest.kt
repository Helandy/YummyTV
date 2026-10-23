package su.afk.yummy.tv.source.amedia.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.amedia.data.dto.AmediaTitleResponseDto
import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle

class AmediaMapperTest : BaseUnitTest() {

    @Test
    fun `request carries every field the server matches on`() {
        val dto = AmediaMapper.toRequest(AmediaLookup(59088, "Мстители", listOf("Revengers"), 2026, "tv", 12))

        assertEquals(59088, dto.malId)
        assertEquals("Мстители", dto.title)
        assertEquals(2026, dto.year)
        assertEquals("tv", dto.type)
        assertEquals(12, dto.episodes)
        assertEquals(true, dto.otherTitles.contains("Revengers"))
    }

    @Test
    fun `only an amd online page of an available title becomes a title`() {
        assertEquals(AmediaTitle("https://amd.online/1-x.html"), AmediaMapper.toTitle(AmediaTitleResponseDto(true, " https://amd.online/1-x.html ")))
        assertNull(AmediaMapper.toTitle(AmediaTitleResponseDto(false, "https://amd.online/1-x.html")))
        assertNull(AmediaMapper.toTitle(AmediaTitleResponseDto(true, null)))
        assertNull(AmediaMapper.toTitle(AmediaTitleResponseDto(true, "https://evil.example/1-x.html")))
    }
}
