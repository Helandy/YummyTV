package su.afk.yummy.tv.feature.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.SubscriptionKeys

/**
 * Состояние подписки приходит от сервера полем `subscribed` у каждого видео. Регресс: раньше оно
 * восстанавливалось сравнением названий озвучек, и одна подписка подсвечивала весь балансер.
 * Названия здесь реальные, из `GET /anime/{id}/videos`.
 */
class SubscriptionOptionMapperTest : BaseUnitTest() {

    private val videos = listOf(
        video(id = 10, episode = "1", dubbing = SUBS, player = KODIK, subscribed = true),
        video(id = 11, episode = "2", dubbing = SUBS, player = KODIK, subscribed = true),
        video(id = 20, episode = "1", dubbing = SUBS_WAKANIM, player = KODIK),
        video(id = 30, episode = "1", dubbing = SUBS_YAKUSUB, player = KODIK),
        video(id = 40, episode = "1", dubbing = SUBS, player = CVH, playerId = 3),
    )

    @Test
    fun `subscription lights up only its own dubbing`() {
        val subscribed = videos.toSubscriptionOptions().filter { it.isSubscribed }

        assertEquals(1, subscribed.size)
        assertEquals(SUBS, subscribed.single().dubbing)
        assertEquals(KODIK, subscribed.single().player)
    }

    @Test
    fun `same dubbing on another balancer stays unsubscribed`() {
        val options = videos.toSubscriptionOptions()

        assertFalse(options.single { it.player == CVH }.isSubscribed)
    }

    @Test
    fun `the subscribed video is used for the request`() {
        val options = videos.toSubscriptionOptions()

        assertEquals(10, options.single { it.key == key(KODIK, SUBS) }.subscriptionVideoId)
    }

    @Test
    fun `without a subscription the latest episode is used`() {
        val options = videos.toSubscriptionOptions()

        assertEquals(30, options.single { it.key == key(KODIK, SUBS_YAKUSUB) }.subscriptionVideoId)
    }

    @Test
    fun `pending state wins while the request is in flight`() {
        val pendingKey = key(KODIK, SUBS_WAKANIM)
        val options = videos.toSubscriptionOptions(pendingStates = mapOf(pendingKey to true))

        assertTrue(options.single { it.key == pendingKey }.isSubscribed)
    }

    @Test
    fun `pending unsubscribe overrides the server flag`() {
        val subscribedKey = key(KODIK, SUBS)
        val options = videos.toSubscriptionOptions(pendingStates = mapOf(subscribedKey to false))

        assertFalse(options.single { it.key == subscribedKey }.isSubscribed)
    }

    @Test
    fun `videos are grouped into one option per dubbing and balancer`() {
        val options = videos.toSubscriptionOptions()

        assertEquals(4, options.size)
        assertEquals(2, options.single { it.key == key(KODIK, SUBS) }.episodesCount)
    }

    private fun key(player: String, dubbing: String): String =
        SubscriptionKeys.subscriptionKey(if (player == KODIK) 4 else 3, player, dubbing)

    private fun video(
        id: Int,
        episode: String,
        dubbing: String,
        player: String,
        playerId: Int = 4,
        subscribed: Boolean = false,
    ) = AnimeVideo(
        id = id,
        episode = episode,
        dubbing = dubbing,
        player = player,
        playerId = playerId,
        iframeUrl = "https://example.test/$id",
        durationSeconds = null,
        isSubscribed = subscribed,
    )

    private companion object {
        const val KODIK = "Плеер Kodik"
        const val CVH = "Плеер CVH"
        const val SUBS = "Субтитры"
        const val SUBS_WAKANIM = "Субтитры Wakanim"
        const val SUBS_YAKUSUB = "Субтитры YakuSub Studio"
    }
}
