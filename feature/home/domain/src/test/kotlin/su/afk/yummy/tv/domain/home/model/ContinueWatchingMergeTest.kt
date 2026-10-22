package su.afk.yummy.tv.domain.home.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.testing.BaseUnitTest

class ContinueWatchingMergeTest : BaseUnitTest() {

    @Test
    fun `remote-only watched episode suggests the next one from the start`() {
        val remote = remote(episode = "2", positionMs = WATCHED_MS)

        val card = emptyList<HomeContinueWatchingItem>().merge(remote).single()

        assertEquals(ContinueWatchingSource.REMOTE_NEXT_EPISODE, card.source)
        assertEquals("3", card.episode)
        assertEquals(0L, card.positionMs)
        assertEquals(0L, card.durationMs)
        assertEquals("", card.screenshotUrl)
        assertEquals(REMOTE_UPDATED_AT, card.updatedAt)
    }

    @Test
    fun `remote watched episode hides the title when next episode suggestion is off`() {
        val remote = remote(episode = "2", positionMs = WATCHED_MS)

        assertTrue(emptyList<HomeContinueWatchingItem>().merge(remote, suggestNextEpisode = false).isEmpty())
    }

    @Test
    fun `newer remote watched episode replaces older local progress of the same episode`() {
        val local = local(episode = "2", positionMs = 300_000L, durationMs = DURATION_MS)
        val remote = remote(episode = "2", positionMs = WATCHED_MS)

        val card = listOf(local).merge(remote).single()

        assertEquals(ContinueWatchingSource.REMOTE_NEXT_EPISODE, card.source)
        assertEquals("3", card.episode)
    }

    @Test
    fun `local next episode target beats a newer remote watched earlier episode`() {
        val local = local(episode = "3", positionMs = 0L, durationMs = 0L)
        val remote = remote(episode = "2", positionMs = WATCHED_MS)

        assertEquals(listOf(local), listOf(local).merge(remote))
    }

    @Test
    fun `newer local entry wins over a remote watched episode`() {
        val local = local(episode = "1", positionMs = 300_000L, durationMs = DURATION_MS)
            .copy(updatedAt = REMOTE_UPDATED_AT + 1)
        val remote = remote(episode = "2", positionMs = WATCHED_MS)

        assertEquals(listOf(local), listOf(local).merge(remote))
    }

    @Test
    fun `next episode number drops leading zeros and fractions`() {
        assertEquals("2", emptyList<HomeContinueWatchingItem>().merge(remote("01", WATCHED_MS)).single().episode)
        assertEquals("8", emptyList<HomeContinueWatchingItem>().merge(remote("7.5", WATCHED_MS)).single().episode)
    }

    @Test
    fun `remote watched episode without a number hides the title`() {
        val remote = remote(episode = "Фильм", positionMs = WATCHED_MS)

        assertTrue(emptyList<HomeContinueWatchingItem>().merge(remote).isEmpty())
    }

    @Test
    fun `manually removed remote watched episode is not suggested`() {
        val remote = remote(episode = "2", positionMs = WATCHED_MS)

        val merged = emptyList<HomeContinueWatchingItem>().merge(
            remote,
            suppressedAt = mapOf(ANIME_ID to REMOTE_UPDATED_AT),
        )

        assertTrue(merged.isEmpty())
    }

    @Test
    fun `remote unfinished episode is shown as is`() {
        val remote = remote(episode = "2", positionMs = 300_000L)

        assertEquals(listOf(remote), emptyList<HomeContinueWatchingItem>().merge(remote))
    }

    @Test
    fun `newer remote mirror of local 4K progress keeps the local 4K card`() {
        val local = local(episode = "2", positionMs = 300_000L, durationMs = DURATION_MS)
            .copy(playerName = "4K", episodeUrl = "source://anime4k/1")
        val remote = remote(episode = "02", positionMs = 305_000L)

        assertEquals(listOf(local), listOf(local).merge(remote))
    }

    @Test
    fun `remote progress far ahead of local 4K progress wins`() {
        val local = local(episode = "2", positionMs = 300_000L, durationMs = DURATION_MS)
            .copy(playerName = "4K", episodeUrl = "source://anime4k/1")
        val remote = remote(episode = "2", positionMs = 600_000L)

        assertEquals(listOf(remote), listOf(local).merge(remote))
    }

    @Test
    fun `card of an episode the server marks watched is replaced by the next episode`() {
        val card = local(episode = "3", positionMs = PARTIAL_MS, durationMs = EPISODE_MS)

        val result = listOf(card).reconcile(server(episode = "3", positionMs = EPISODE_MS - 10_000L))

        assertEquals("4", result.single().episode)
        assertEquals(ContinueWatchingSource.REMOTE_NEXT_EPISODE, result.single().source)
    }

    @Test
    fun `card of a watched episode is hidden when next episode suggestion is off`() {
        val card = local(episode = "3", positionMs = PARTIAL_MS, durationMs = EPISODE_MS)

        val result = listOf(card).reconcile(
            server(episode = "3", positionMs = EPISODE_MS - 10_000L),
            suggestNextEpisode = false,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `newer local progress beats an older server watched record`() {
        val card = local(episode = "3", positionMs = PARTIAL_MS, durationMs = EPISODE_MS)
            .copy(updatedAt = SERVER_UPDATED_AT + 1)

        val result = listOf(card).reconcile(
            server(episode = "3", positionMs = EPISODE_MS - 10_000L),
            history = listOf(card),
        )

        assertEquals(listOf(card), result)
    }

    @Test
    fun `server record of another episode leaves the card alone`() {
        val card = local(episode = "3", positionMs = PARTIAL_MS, durationMs = EPISODE_MS)

        assertEquals(listOf(card), listOf(card).reconcile(server(episode = "2", positionMs = EPISODE_MS)))
    }

    @Test
    fun `titles without server progress are left alone`() {
        val card = local(episode = "3", positionMs = PARTIAL_MS, durationMs = EPISODE_MS)

        assertEquals(listOf(card), listOf(card).reconcile())
    }

    @Test
    fun `leading zero in the server episode still matches the card`() {
        val card = local(episode = "3", positionMs = PARTIAL_MS, durationMs = EPISODE_MS)

        val result = listOf(card).reconcile(
            server(episode = "03", positionMs = EPISODE_MS),
            suggestNextEpisode = false,
        )

        assertTrue(result.isEmpty())
    }

    private fun List<HomeContinueWatchingItem>.reconcile(
        vararg server: AnimeWatchProgress,
        history: List<HomeContinueWatchingItem> = this,
        suggestNextEpisode: Boolean = true,
    ) = reconcileWithEpisodeProgress(
        serverProgress = server.toList(),
        localHistory = history,
        suggestNextEpisode = suggestNextEpisode,
    )

    private fun server(episode: String, positionMs: Long) = AnimeWatchProgress(
        animeId = ANIME_ID,
        episode = episode,
        videoId = 99,
        episodeUrl = "https://kodik.info/seria/99",
        positionMs = positionMs,
        durationMs = EPISODE_MS,
        updatedAt = SERVER_UPDATED_AT,
    )

    private fun List<HomeContinueWatchingItem>.merge(
        remote: HomeContinueWatchingItem,
        suppressedAt: Map<Int, Long> = emptyMap(),
        suggestNextEpisode: Boolean = true,
    ) = mergeWithRemote(
        remote = listOf(remote),
        localHistory = this,
        suppressedAt = suppressedAt,
        suggestNextEpisode = suggestNextEpisode,
    )

    private fun local(episode: String, positionMs: Long, durationMs: Long) =
        item(episode, positionMs, durationMs).copy(
            videoId = 10,
            episodeUrl = "https://kodik.info/seria/10",
            updatedAt = LOCAL_UPDATED_AT,
        )

    private fun remote(episode: String, positionMs: Long) =
        item(episode, positionMs, DURATION_MS).copy(
            updatedAt = REMOTE_UPDATED_AT,
            screenshotUrl = "https://static.yani.tv/screenshot.jpg",
            source = ContinueWatchingSource.REMOTE,
        )

    private fun item(episode: String, positionMs: Long, durationMs: Long) = HomeContinueWatchingItem(
        animeId = ANIME_ID,
        animeTitle = "Title",
        description = "",
        poster = null,
        videoId = 0,
        episode = episode,
        episodeUrl = "",
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = 0L,
        playerName = "",
        dubbing = "",
        screenshotUrl = "",
    )

    private companion object {
        const val ANIME_ID = 7
        const val DURATION_MS = 1_440_000L
        const val WATCHED_MS = 1_430_000L
        const val LOCAL_UPDATED_AT = 1_000_000L
        const val REMOTE_UPDATED_AT = 2_000_000L
        const val SERVER_UPDATED_AT = 3_000_000L
        const val EPISODE_MS = 729_000L // 12:09
        const val PARTIAL_MS = 423_000L // 7:03
    }
}
