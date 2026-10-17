package su.afk.yummy.tv.data.home.storage.mapper

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.data.home.dto.YaniAnimeDto
import su.afk.yummy.tv.data.home.dto.YaniFeedDto
import su.afk.yummy.tv.data.home.dto.YaniFeedResponseDto
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType

class HomeStorageMapperTest {

    private val stringProvider: StringProvider = mockk {
        every { get(any<Int>()) } returns "title"
    }

    @Test
    fun `announcements and new titles become their own sections`() {
        val feed = YaniFeedDto(
            YaniFeedResponseDto(
                announcements = listOf(anime(1), anime(2)),
                new = listOf(anime(3)),
                recommends = listOf(anime(4)),
            ),
        ).toHomeFeedCache(language = "ru", watchSignature = "", cachedAt = 0L)
            .toHomeFeed(stringProvider)

        assertEquals(
            listOf(
                HomeFeedSectionType.NEW_TITLES,
                HomeFeedSectionType.RECOMMENDATIONS,
                HomeFeedSectionType.ANNOUNCEMENTS,
            ),
            feed.sections.map { it.type },
        )
        assertEquals(
            listOf(1, 2),
            feed.sections.single { it.type == HomeFeedSectionType.ANNOUNCEMENTS }.items.map { it.id },
        )
        assertEquals(
            listOf(3),
            feed.sections.single { it.type == HomeFeedSectionType.NEW_TITLES }.items.map { it.id },
        )
    }

    private fun anime(id: Int) = YaniAnimeDto(animeId = id, title = "Anime $id")
}
