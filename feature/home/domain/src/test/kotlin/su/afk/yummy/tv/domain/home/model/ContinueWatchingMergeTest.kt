package su.afk.yummy.tv.domain.home.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
    }
}
