package su.afk.yummy.tv.data.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.data.details.dto.YummyEpisodeDto
import su.afk.yummy.tv.data.details.dto.YummyEpisodesDto
import su.afk.yummy.tv.data.details.dto.YummyImageDto

class YummyImagesMapperTest {

    private fun image(path: String) = YummyImageDto(url = "https://image.tmdb.org/t/p/original$path", filePath = path)

    @Test
    fun `stills of the first episode are requested in w1280 and keep the order`() {
        val dto = YummyEpisodesDto(
            episodes = listOf(
                YummyEpisodeDto(episodeNumber = 1, stills = listOf(image("/s1.jpg"), image("/s2.jpg"))),
                YummyEpisodeDto(episodeNumber = 2, stills = listOf(image("/other.jpg"))),
            ),
        )

        val images = dto.toAnimeTmdbImages()

        assertEquals(
            listOf("https://image.tmdb.org/t/p/w1280/s1.jpg", "https://image.tmdb.org/t/p/w1280/s2.jpg"),
            images.firstEpisodeStillUrls,
        )
    }

    @Test
    fun `episode previews are keyed by episode group key and kept as served`() {
        val dto = YummyEpisodesDto(
            episodes = listOf(
                YummyEpisodeDto(episodeNumber = 1, previewUrl = "https://image.tmdb.org/t/p/w300/a.jpg"),
                YummyEpisodeDto(episodeNumber = 2, previewUrl = null),
                YummyEpisodeDto(episodeNumber = 3, previewUrl = " "),
                YummyEpisodeDto(episodeNumber = 0, previewUrl = "https://image.tmdb.org/t/p/w300/zero.jpg"),
            ),
        )

        assertEquals(mapOf("1" to "https://image.tmdb.org/t/p/w300/a.jpg"), dto.toAnimeTmdbImages().episodePreviewUrls)
    }

    @Test
    fun `mirror urls are mapped separately from tmdb previews`() {
        val dto = YummyEpisodesDto(
            episodes = listOf(
                YummyEpisodeDto(episodeNumber = 1, previewUrl = "https://image.tmdb.org/t/p/w300/a.jpg", previewMirrorUrl = "https://static.afk.su/img/still/a.avif"),
                YummyEpisodeDto(episodeNumber = 2, previewUrl = "https://image.tmdb.org/t/p/w300/b.jpg"),
            ),
        )

        assertEquals(mapOf("1" to "https://static.afk.su/img/still/a.avif"), dto.toAnimeTmdbImages().episodeMirrorUrls)
    }

    @Test
    fun `missing images give an empty model`() {
        val images = YummyEpisodesDto().toAnimeTmdbImages()

        assertTrue(images.firstEpisodeStillUrls.isEmpty())
        assertTrue(images.episodePreviewUrls.isEmpty())
    }

    @Test
    fun `url of an unknown shape is used as is and blank ones are dropped`() {
        val dto = YummyEpisodesDto(
            episodes = listOf(
                YummyEpisodeDto(
                    episodeNumber = 1,
                    stills = listOf(
                        YummyImageDto(url = "https://cdn.example/x.jpg", filePath = "/x.jpg"),
                        YummyImageDto(url = "", filePath = "/y.jpg"),
                    ),
                ),
            ),
        )

        assertEquals(listOf("https://cdn.example/x.jpg"), dto.toAnimeTmdbImages().firstEpisodeStillUrls)
    }
}
