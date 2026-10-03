package su.afk.yummy.tv.data.account.dto

import su.afk.yummy.tv.data.account.dto.lists.YaniUserAnimeDto
import su.afk.yummy.tv.data.account.dto.lists.YaniUserListResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.network.yani.YaniApiJson
import su.afk.yummy.tv.data.account.storage.mapper.toUserListCache

/**
 * Дата выхода следующей серии в библиотеке берётся из плоского поля `next_episode` ответа
 * `/users/{id}/lists` — проверяем, что она доезжает до кэша, а её отсутствие или ноль
 * дают `null` (иначе на карточке появился бы отсчёт от эпохи).
 */
class YaniUserListNextEpisodeParsingTest {

    @Test
    fun `next_episode reaches the cached list item`() {
        val items = decodeItems().toUserListCache(
            userId = 1,
            listId = 1,
            language = "ru",
            cachedAt = 0L,
        ).items

        assertEquals(1_756_400_000L, items[0].nextEpisodeAtSeconds)
    }

    @Test
    fun `zero and missing next_episode are read as no date`() {
        val items = decodeItems().toUserListCache(
            userId = 1,
            listId = 1,
            language = "ru",
            cachedAt = 0L,
        ).items

        assertNull(items[1].nextEpisodeAtSeconds)
        assertNull(items[2].nextEpisodeAtSeconds)
        assertEquals(3, items.size)
    }

    private fun decodeItems(): List<YaniUserAnimeDto> =
        YaniApiJson.decodeFromString<YaniUserListResponseDto>(USER_LIST_JSON).response

    private companion object {
        // Форма ответа /users/{id}/lists: у онгоинга next_episode — дата будущей серии,
        // у части строк там ноль, а у большинства завершённых тайтлов поля нет вовсе.
        val USER_LIST_JSON = """
            {"response":[
              {"anime_id":26429,"title":"Онгоинг","year":2026,"next_episode":1756400000},
              {"anime_id":1274,"title":"Нулевая дата","year":2019,"next_episode":0},
              {"anime_id":777,"title":"Без даты","year":2020}
            ]}
        """.trimIndent()
    }
}
