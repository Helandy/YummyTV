package su.afk.yummy.tv.data.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.data.details.dto.MalEpisodeDto
import su.afk.yummy.tv.data.details.dto.MalEpisodesDto

class MalEpisodeFlagsMapperTest {

    @Test
    fun `keeps only flagged episodes keyed by number`() {
        val dto = MalEpisodesDto(
            data = listOf(
                MalEpisodeDto(episodeNumber = 1),
                MalEpisodeDto(episodeNumber = 2, filler = true),
                MalEpisodeDto(episodeNumber = 3, recap = true),
                MalEpisodeDto(episodeNumber = 0, filler = true),
            ),
        )

        assertEquals(
            mapOf(
                "2" to EpisodeFlags(filler = true, recap = false),
                "3" to EpisodeFlags(filler = false, recap = true),
            ),
            dto.toEpisodeFlagsByNumber(),
        )
    }
}
