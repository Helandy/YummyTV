package su.afk.yummy.tv.data.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.data.details.dto.EpisodeFlagsBackupDto
import su.afk.yummy.tv.data.details.dto.EpisodeFlagsBackupEpisodeDto

class EpisodeFlagsBackupMapperTest {

    @Test
    fun `backup flags give the same result as the Tenrai document`() {
        val dto = EpisodeFlagsBackupDto(
            malId = 52991,
            episodes = listOf(
                EpisodeFlagsBackupEpisodeDto(number = 2, filler = true),
                EpisodeFlagsBackupEpisodeDto(number = 3, recap = true),
            ),
        )

        assertEquals(
            mapOf(
                "2" to EpisodeFlags(filler = true, recap = false),
                "3" to EpisodeFlags(filler = false, recap = true),
            ),
            dto.toMalEpisodesDto().toEpisodeFlagsByNumber(),
        )
    }

    @Test
    fun `empty backup gives no flags`() {
        assertEquals(emptyMap<String, EpisodeFlags>(), EpisodeFlagsBackupDto().toMalEpisodesDto().toEpisodeFlagsByNumber())
    }
}
