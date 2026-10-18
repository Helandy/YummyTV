package su.afk.yummy.tv.data.account.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus
import su.afk.yummy.tv.core.network.yani.YaniApiJson
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.account.dto.lists.YaniUserListResponseDto
import su.afk.yummy.tv.data.account.storage.mapper.toUserListCache

/**
 * Тип (`type.shortname`) и статус выхода (`anime_status.alias`) из `/users/{id}/lists` доходят
 * до кэша; тип «Неизвестно» и незнакомый статус не должны попадать на карточку подписью.
 */
class YaniUserListKindStatusParsingTest : BaseUnitTest() {

    @Test
    fun `kind and release status reach the cached list item`() {
        val item = cachedItems()[0]

        assertEquals("ТВ", item.kind)
        assertEquals(AnimeReleaseStatus.ONGOING.slug, item.releaseStatus)
    }

    @Test
    fun `announcement and movie are parsed`() {
        val item = cachedItems()[1]

        assertEquals("п/ф", item.kind)
        assertEquals(AnimeReleaseStatus.ANNOUNCEMENT.slug, item.releaseStatus)
    }

    @Test
    fun `unknown type and status are read as missing`() {
        val items = cachedItems()

        assertNull(items[2].kind)
        assertNull(items[2].releaseStatus)
        assertNull(items[3].kind)
        assertNull(items[3].releaseStatus)
        assertEquals(4, items.size)
    }

    private fun cachedItems() =
        YaniApiJson.decodeFromString<YaniUserListResponseDto>(USER_LIST_JSON).response
            .toUserListCache(userId = 1, listId = 1, language = "ru", cachedAt = 0L)
            .items

    private companion object {
        val USER_LIST_JSON = """
            {"response":[
              {"anime_id":1,"title":"Сериал",
               "type":{"name":"Сериал","value":1,"shortname":"ТВ","alias":"tv"},
               "anime_status":{"value":1,"title":"онгоинг","alias":"ongoing","class":"review"}},
              {"anime_id":2,"title":"Фильм",
               "type":{"name":"Полнометражный фильм","value":2,"shortname":"п/ф","alias":"movie"},
               "anime_status":{"value":2,"title":"анонс","alias":"announcement","class":"attention"}},
              {"anime_id":3,"title":"Неизвестно",
               "type":{"name":"Неизвестно","value":0,"shortname":"Неизвестно","alias":"unknown"},
               "anime_status":{"value":9,"alias":"paused"}},
              {"anime_id":4,"title":"Без полей"}
            ]}
        """.trimIndent()
    }
}
