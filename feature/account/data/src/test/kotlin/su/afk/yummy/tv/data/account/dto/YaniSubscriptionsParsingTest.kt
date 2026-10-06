package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.network.yani.YaniApiJson
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.account.dto.video.YaniVideoSubscriptionsResponseDto

/**
 * Регресс: yani отдаёт `sub.dubbing` то строкой, то `null`. Поле объявлено non-null с дефолтом,
 * и без `coerceInputValues` в [YaniApiJson] такой ответ ронял разбор целиком —
 * экран «Мои подписки» показывал текст исключения вместо списка.
 */
class YaniSubscriptionsParsingTest : BaseUnitTest() {

    @Test
    fun `null dubbing is read as an empty string`() {
        val response = YaniApiJson.decodeFromString<YaniVideoSubscriptionsResponseDto>(SUBS_JSON)

        val withNullDubbing = response.response[1]
        assertEquals("", withNullDubbing.sub?.dubbing)
        assertEquals("Kodik", withNullDubbing.sub?.player)
    }

    @Test
    fun `a normal row is still parsed`() {
        val response = YaniApiJson.decodeFromString<YaniVideoSubscriptionsResponseDto>(SUBS_JSON)

        val first = response.response[0]
        assertEquals("Табакошка", first.title)
        assertEquals("tabakoshka", first.animeUrl)
        assertEquals("Alloha", first.sub?.player)
        assertEquals(JsonPrimitive(26429), first.animeId)
    }

    @Test
    fun `null strings elsewhere in the row do not break the response`() {
        val response = YaniApiJson.decodeFromString<YaniVideoSubscriptionsResponseDto>(SUBS_JSON)

        val withNulls = response.response[2]
        assertEquals("", withNulls.title)
        assertEquals("", withNulls.animeUrl)
        assertEquals("", withNulls.sub?.player)
        assertEquals(3, response.response.size)
    }

    private companion object {
        // Форма ответа /users/{id}/lists/subs: anime_id приходит то числом, то строкой,
        // а dubbing — то перечислением озвучек плеера, то пустой строкой, то null.
        val SUBS_JSON = """
            {"response":[
              {"title":"Табакошка","anime_id":26429,"anime_url":"tabakoshka",
               "sub":{"dubbing":"AniLibria() AniDUB() ","player":"Alloha","player_id":2}},
              {"title":"Сага о Винланде","anime_id":"1274","anime_url":"saga-o-vinlande",
               "sub":{"dubbing":null,"player":"Kodik","player_id":4}},
              {"title":null,"anime_id":null,"anime_url":null,
               "sub":{"dubbing":null,"player":null,"player_id":null}}
            ]}
        """.trimIndent()
    }
}
