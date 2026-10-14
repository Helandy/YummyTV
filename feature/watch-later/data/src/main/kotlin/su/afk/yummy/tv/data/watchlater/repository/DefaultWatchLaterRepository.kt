package su.afk.yummy.tv.data.watchlater.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.storage.watchlater.WatchLaterStorage
import su.afk.yummy.tv.data.watchlater.mapper.toDomain
import su.afk.yummy.tv.domain.watchlater.model.WatchLaterItem
import su.afk.yummy.tv.domain.watchlater.repository.WatchLaterRepository
import javax.inject.Inject

/**
 * Номер серии нормализуется на входе и на выходе: разные озвучки присылают его по-разному
 * («01» против «1»), иначе одна и та же серия попадёт в список дважды.
 */
internal class DefaultWatchLaterRepository @Inject constructor(
    private val storage: WatchLaterStorage,
) : WatchLaterRepository {

    override fun observeAll(): Flow<List<WatchLaterItem>> =
        storage.observeAll().map { entries -> entries.map { it.toDomain() } }

    override fun observeEpisodes(animeId: Int): Flow<Set<String>> =
        storage.observeByAnimeId(animeId).map { entries ->
            entries.mapTo(mutableSetOf()) { it.episode.episodeGroupKey() }
        }

    override suspend fun add(item: WatchLaterItem) {
        storage.save(
            animeId = item.animeId,
            episode = item.episode.episodeGroupKey(),
            animeTitle = item.animeTitle,
            posterUrl = item.posterUrl,
            screenshotUrl = item.screenshotUrl,
            addedAt = item.addedAt,
        )
    }

    override suspend fun remove(animeId: Int, episode: String) {
        storage.delete(animeId, episode.episodeGroupKey())
    }

    override suspend fun pruneWatched() = storage.pruneWatched()
}
