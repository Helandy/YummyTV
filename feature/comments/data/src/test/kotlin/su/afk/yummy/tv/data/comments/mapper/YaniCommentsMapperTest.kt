package su.afk.yummy.tv.data.comments.mapper

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.storage.comments.CommentsPageCache
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.comments.dto.YaniCommentDto
import su.afk.yummy.tv.data.comments.dto.YaniCommentsResponseDto
import su.afk.yummy.tv.domain.comments.model.CommentItem

class YaniCommentsMapperTest : BaseUnitTest() {

    private val json = Json { ignoreUnknownKeys = true }

    // Фрагмент реального ответа /comments/user/1: ответ на чужой комментарий под тайтлом.
    private val userCommentsJson = """
        {"response":{"comments":[{"avatars":{"small":"//static.yani.tv/users/small/1.webp?v=4"},
        "text":"[ник]vadcapi[/ник] за первые 15 серий втянешься)","id":6514,"name":"root",
        "children_count":0,"deleted_at":0,"dislikes":2,"object_id":619,"likes":9,"time":1692454737,
        "parent_id":12,"roles":["root"],"user_id":1,"vote":0,
        "item_data":{"posters":{"small":"//static.yani.tv/posters/small/1636668094.webp",
        "medium":"//static.yani.tv/posters/medium/1636668094.webp"},
        "title":"Когда плачут цикады","url":"/catalog/item/kogda-plachut-cikady-tv-1"}}],"isMod":false}}
    """.trimIndent()

    private val expectedItem = CommentItem(
        animeId = 619,
        title = "Когда плачут цикады",
        posterUrl = "https://static.yani.tv/posters/medium/1636668094.webp",
    )

    @Test
    fun `item data maps to comment item with medium poster`() {
        val dto = json.decodeFromString<YaniCommentsResponseDto>(userCommentsJson).response.comments.single()

        assertEquals(expectedItem, dto.toComment().item)
    }

    @Test
    fun `item survives cache round trip`() {
        val response = json.decodeFromString<YaniCommentsResponseDto>(userCommentsJson)
        val cache: CommentsPageCache = response.toCommentsPageCache(
            scopeType = "user",
            ownerId = 1,
            sort = "new",
            limit = 10,
            skip = 0,
            cachedAt = 0L,
        )

        assertEquals(expectedItem, cache.toCommentsPage().comments.single().item)
    }

    @Test
    fun `comment without item data has no item`() {
        assertNull(YaniCommentDto(id = 1, text = "hi").toComment().item)
    }
}
