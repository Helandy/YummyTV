package su.afk.yummy.tv.data.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.data.details.dto.MalEpisodeDto
import su.afk.yummy.tv.data.details.dto.MalEpisodeImageDto
import su.afk.yummy.tv.data.details.dto.MalEpisodeImagesDto
import su.afk.yummy.tv.data.details.dto.MalEpisodesDto

class MalEpisodeThumbnailsMapperTest {

    private fun episode(number: Int, url: String?) = MalEpisodeDto(
        episodeNumber = number,
        images = MalEpisodeImagesDto(jpg = MalEpisodeImageDto(imageUrl = url)),
    )

    @Test
    fun `keeps episodes with image keyed by number`() {
        val dto = MalEpisodesDto(
            data = listOf(
                episode(1, "https://cdn/1.jpg"),
                episode(2, null),
                episode(3, " "),
                episode(0, "https://cdn/0.jpg"),
            ),
        )

        assertEquals(mapOf("1" to "https://cdn/1.jpg"), dto.toThumbnailUrlByNumber())
    }
}
