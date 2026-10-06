package su.afk.yummy.tv.data.account.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeSeason
import su.afk.yummy.tv.core.network.yani.YaniApiJson
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.account.dto.lists.YaniUserListResponseDto
import su.afk.yummy.tv.data.account.storage.mapper.toUserListCache

/**
 * Сезон выхода в ответе `/users/{id}/lists` приходит то числом 1..4, то слагом, поэтому
 * проверяем оба варианта, а мусор и отсутствие поля должны давать `null` — иначе на карточке
 * рядом с годом появилась бы пустая или неверная подпись.
 */
class YaniUserListSeasonParsingTest : BaseUnitTest() {

    @Test
    fun `numeric and slug seasons reach the cached list item`() {
        val items = cachedItems()

        assertEquals(AnimeSeason.WINTER.slug, items[0].season)
        assertEquals(AnimeSeason.FALL.slug, items[1].season)
        assertEquals(AnimeSeason.SUMMER.slug, items[2].season)
    }

    @Test
    fun `unknown and missing seasons are read as no season`() {
        val items = cachedItems()

        assertNull(items[3].season)
        assertNull(items[4].season)
        assertNull(items[5].season)
        assertNull(items[6].season)
        assertEquals(7, items.size)
    }

    private fun cachedItems() =
        YaniApiJson.decodeFromString<YaniUserListResponseDto>(USER_LIST_JSON).response
            .toUserListCache(userId = 1, listId = 1, language = "ru", cachedAt = 0L)
            .items

    private companion object {
        val USER_LIST_JSON = """
            {"response":[
              {"anime_id":1,"title":"Число","year":2024,"season":1},
              {"anime_id":2,"title":"Число","year":2024,"season":4},
              {"anime_id":3,"title":"Слаг","year":2024,"season":"summer"},
              {"anime_id":4,"title":"Мусор","year":2024,"season":"unknown"},
              {"anime_id":5,"title":"Вне диапазона","year":2024,"season":9},
              {"anime_id":6,"title":"Нулевой сезон","year":2024,"season":0},
              {"anime_id":7,"title":"Без сезона","year":2024}
            ]}
        """.trimIndent()
    }
}
