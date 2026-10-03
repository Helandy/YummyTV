package su.afk.yummy.tv.feature.details.episodes

import su.afk.yummy.tv.core.utils.coroutines.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.domain.watchlater.model.WatchLaterItem
import su.afk.yummy.tv.domain.watchlater.repository.WatchLaterRepository
import su.afk.yummy.tv.domain.watchlater.usecase.AddWatchLaterEpisodeUseCase
import su.afk.yummy.tv.domain.watchlater.usecase.RemoveWatchLaterEpisodeUseCase
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeWatchLaterHandler
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeWatchedHandler

/** Пометка «отложить просмотр» — переключатель, сервера у него нет. */
class EpisodeWatchLaterHandlerTest {

    private val repository = FakeWatchLaterRepository()

    private val handler = EpisodeWatchLaterHandler(
        addWatchLaterEpisode = AddWatchLaterEpisodeUseCase(repository, FixedClock),
        removeWatchLaterEpisode = RemoveWatchLaterEpisodeUseCase(repository),
    )

    private val meta = EpisodeWatchedHandler.EpisodeMeta(
        animeTitle = "Title",
        posterUrl = "poster",
        screenshotUrl = "shot",
    )

    @Test
    fun `adds episode with title metadata when it is not postponed yet`() = runTest {
        handler.toggle(animeId = 7, episode = "3", isInWatchLater = false, meta = meta)

        val added = repository.added.single()
        assertEquals(7, added.animeId)
        assertEquals("3", added.episode)
        assertEquals("Title", added.animeTitle)
        assertEquals("shot", added.screenshotUrl)
        assertTrue(repository.removed.isEmpty())
    }

    @Test
    fun `removes episode when it is already postponed`() = runTest {
        handler.toggle(animeId = 7, episode = "3", isInWatchLater = true, meta = meta)

        assertEquals(7 to "3", repository.removed.single())
        assertTrue(repository.added.isEmpty())
    }

    private class FakeWatchLaterRepository : WatchLaterRepository {
        val added = mutableListOf<WatchLaterItem>()
        val removed = mutableListOf<Pair<Int, String>>()

        override fun observeAll(): Flow<List<WatchLaterItem>> = flowOf(added.toList())

        override fun observeEpisodes(animeId: Int): Flow<Set<String>> =
            flowOf(added.filter { it.animeId == animeId }.mapTo(mutableSetOf()) { it.episode })

        override suspend fun add(item: WatchLaterItem) {
            added += item
        }

        override suspend fun remove(animeId: Int, episode: String) {
            removed += animeId to episode
        }

        override suspend fun pruneWatched() = Unit
    }
}

internal object FixedClock : AppClock {
    override fun nowMillis(): Long = 1_000L
}
