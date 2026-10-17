package su.afk.yummy.tv.core.storage.watchlater

import kotlinx.coroutines.flow.Flow

/**
 * Список отложенных серий чисто ручной: запись появляется по действию «Отложить просмотр»
 * и исчезает только когда пользователь сам её убирает. Прогресс просмотра на список не влияет.
 */
internal class WatchLaterStore(
    private val dao: WatchLaterDao,
) : WatchLaterStorage {

    override fun observeAll(): Flow<List<WatchLaterEntry>> = dao.observeAll()

    override fun observeByAnimeId(animeId: Int): Flow<List<WatchLaterEntry>> =
        dao.observeByAnimeId(animeId)

    override suspend fun save(
        animeId: Int,
        episode: String,
        animeTitle: String,
        posterUrl: String,
        screenshotUrl: String,
        addedAt: Long,
    ) {
        dao.save(
            WatchLaterEntry(
                animeId = animeId,
                episode = episode,
                animeTitle = animeTitle,
                posterUrl = posterUrl,
                screenshotUrl = screenshotUrl,
                addedAt = addedAt,
            )
        )
    }

    override suspend fun delete(animeId: Int, episode: String) = dao.delete(animeId, episode)

    override suspend fun deleteByAnimeId(animeId: Int) = dao.deleteByAnimeId(animeId)
}
