package su.afk.yummy.tv.external.tenrai.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.external.tenrai.dto.TenraiEpisodeDto

class TenraiEpisodeFlagsMapperTest {

    @Test
    fun `keeps only flagged episodes keyed by number`() {
        val episodes = listOf(
            TenraiEpisodeDto(episodeNumber = 1),
            TenraiEpisodeDto(episodeNumber = 2, filler = true),
            TenraiEpisodeDto(episodeNumber = 3, recap = true),
            TenraiEpisodeDto(episodeNumber = 0, filler = true),
        )

        assertEquals(
            mapOf(
                "2" to EpisodeFlags(filler = true, recap = false),
                "3" to EpisodeFlags(filler = false, recap = true),
            ),
            episodes.toEpisodeFlagsByNumber(),
        )
    }
}
