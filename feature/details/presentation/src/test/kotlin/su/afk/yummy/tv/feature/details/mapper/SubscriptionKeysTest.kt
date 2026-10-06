package su.afk.yummy.tv.feature.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.SubscriptionKeys

/**
 * `/anime/{id}/videos` отдаёт плеер как «Плеер Kodik», а `/users/{id}/lists/subs` — как «Kodik»,
 * поэтому ключ плеера должен совпадать в обоих написаниях.
 */
class SubscriptionKeysTest : BaseUnitTest() {

    @Test
    fun `player id wins over the name spelling`() {
        assertEquals(
            SubscriptionKeys.playerKey(4, "Плеер Kodik"),
            SubscriptionKeys.playerKey(4, "Kodik"),
        )
    }

    @Test
    fun `player name matches with and without the prefix`() {
        assertEquals(
            SubscriptionKeys.playerKey(null, "Плеер Kodik"),
            SubscriptionKeys.playerKey(null, "Kodik"),
        )
    }

    @Test
    fun `dubbings with a common prefix stay different`() {
        assertNotEquals(
            SubscriptionKeys.dubbingKey("Субтитры"),
            SubscriptionKeys.dubbingKey("Субтитры Wakanim"),
        )
        assertNotEquals(
            SubscriptionKeys.dubbingKey("Озвучка Kansai"),
            SubscriptionKeys.dubbingKey("Озвучка KANSAI Studio"),
        )
    }

    @Test
    fun `case and yo are normalized`() {
        assertEquals(
            SubscriptionKeys.dubbingKey("Озвучка СВ-Дубль"),
            SubscriptionKeys.dubbingKey("озвучка св-дубль"),
        )
    }
}
