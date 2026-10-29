package su.afk.yummy.tv.data.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeTypeKind
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.details.dto.YaniAnimeTypeDto

class YaniAnimeTypeKindMapperTest : BaseUnitTest() {

    @Test
    fun `alias decides the kind`() {
        assertEquals(AnimeTypeKind.TV, YaniAnimeTypeDto(alias = "tv").toAnimeTypeKind())
        assertEquals(AnimeTypeKind.MOVIE, YaniAnimeTypeDto(alias = "movie").toAnimeTypeKind())
        assertEquals(AnimeTypeKind.SPECIAL, YaniAnimeTypeDto(alias = "special").toAnimeTypeKind())
        assertEquals(AnimeTypeKind.SHORT_MOVIE, YaniAnimeTypeDto(alias = "short-movie").toAnimeTypeKind())
        assertEquals(AnimeTypeKind.OVA, YaniAnimeTypeDto(alias = "ova").toAnimeTypeKind())
        assertEquals(AnimeTypeKind.OTHER, YaniAnimeTypeDto(alias = "new-format").toAnimeTypeKind())
    }

    @Test
    fun `code is used when alias is missing`() {
        assertEquals(AnimeTypeKind.TV, YaniAnimeTypeDto(value = 1).toAnimeTypeKind())
        assertEquals(AnimeTypeKind.MOVIE, YaniAnimeTypeDto(value = 2).toAnimeTypeKind())
        assertEquals(AnimeTypeKind.SPECIAL, YaniAnimeTypeDto(value = 5).toAnimeTypeKind())
        assertEquals(AnimeTypeKind.OTHER, YaniAnimeTypeDto(value = 99).toAnimeTypeKind())
    }

    @Test
    fun `unknown or absent type gives null`() {
        assertNull(YaniAnimeTypeDto(alias = "unknown", value = 0).toAnimeTypeKind())
        assertNull(YaniAnimeTypeDto().toAnimeTypeKind())
        assertNull(null.toAnimeTypeKind())
    }
}
