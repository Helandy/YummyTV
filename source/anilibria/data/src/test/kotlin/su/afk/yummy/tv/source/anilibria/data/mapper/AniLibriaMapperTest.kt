package su.afk.yummy.tv.source.anilibria.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaEpisodeDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaReleaseDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaSkipDto

class AniLibriaMapperTest : BaseUnitTest() {

    @Test
    fun `fractional, empty and linkless episodes are dropped, rest sorted`() {
        val dto = AniLibriaReleaseDto(
            id = 1,
            episodes = listOf(
                AniLibriaEpisodeDto(ordinal = 2.0, hls_720 = "b"),
                AniLibriaEpisodeDto(ordinal = 1.0, hls_480 = "a", opening = AniLibriaSkipDto(1.0, 89.0)),
                AniLibriaEpisodeDto(ordinal = 1.5, hls_480 = "x"),
                AniLibriaEpisodeDto(ordinal = 3.0),
                AniLibriaEpisodeDto(ordinal = null, hls_480 = "y"),
            ),
        )
        val release = AniLibriaMapper.toRelease(52991, dto)
        assertEquals(listOf(1, 2), release.episodes.map { it.ordinal })
        assertEquals(1, release.episodes[0].opening?.startSec)
        assertEquals(89, release.episodes[0].opening?.endSec)
        assertNull(release.episodes[1].opening)
    }

    @Test
    fun `invalid skip is ignored`() {
        val dto = AniLibriaReleaseDto(
            id = 1,
            episodes = listOf(AniLibriaEpisodeDto(ordinal = 1.0, hls_480 = "a", opening = AniLibriaSkipDto(50.0, 10.0))),
        )
        assertNull(AniLibriaMapper.toRelease(1, dto).episodes[0].opening)
    }
}
