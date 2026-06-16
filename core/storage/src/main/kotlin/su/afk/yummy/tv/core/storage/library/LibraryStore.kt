package su.afk.yummy.tv.core.storage.library

import kotlinx.coroutines.flow.Flow

class LibraryStore(private val dao: LibraryDao) {
    fun observeAll(): Flow<List<LibraryEntry>> = dao.observeAll()
    suspend fun getAll(): List<LibraryEntry> = dao.getAll()
    fun observeIsInLibrary(animeId: Int): Flow<Boolean> = dao.observeIsInLibrary(animeId)
    fun observeIsFavorite(animeId: Int): Flow<Boolean> = dao.observeIsFavorite(animeId)
    suspend fun add(entry: LibraryEntry) = dao.add(entry)
    suspend fun hasSyncState(userId: Int): Boolean = dao.hasSyncState(userId)
    suspend fun markSynced(userId: Int) = dao.saveSyncState(LibrarySyncStateEntry(userId = userId))

    suspend fun refreshMetadata(
        animeId: Int,
        title: String,
        poster: LibraryPoster?,
    ) {
        val entry = dao.getByAnimeId(animeId) ?: return
        dao.add(
            entry.copy(
                title = title.ifBlank { entry.title },
                posterSmallUrl = poster?.small ?: entry.posterSmallUrl,
                posterMediumUrl = poster?.medium ?: entry.posterMediumUrl,
                posterBigUrl = poster?.big ?: entry.posterBigUrl,
                posterFullsizeUrl = poster?.fullsize ?: entry.posterFullsizeUrl,
                posterMegaUrl = poster?.mega ?: entry.posterMegaUrl,
            )
        )
    }

    suspend fun remove(animeId: Int) {
        val entry = dao.getByAnimeId(animeId) ?: return
        if (entry.isFavorite) {
            dao.add(
                entry.copy(
                    listId = FAVORITE_ONLY_LIST_ID,
                    listUpdatedAt = System.currentTimeMillis(),
                )
            )
        } else {
            dao.delete(animeId)
        }
    }

    suspend fun delete(animeId: Int) = dao.delete(animeId)

    suspend fun setFavorite(
        animeId: Int,
        title: String,
        poster: LibraryPoster?,
        favorite: Boolean,
    ) {
        val entry = dao.getByAnimeId(animeId)
        if (favorite) {
            dao.add(
                entry?.copy(
                    title = title.ifBlank { entry.title },
                    posterSmallUrl = poster?.small ?: entry.posterSmallUrl,
                    posterMediumUrl = poster?.medium ?: entry.posterMediumUrl,
                    posterBigUrl = poster?.big ?: entry.posterBigUrl,
                    posterFullsizeUrl = poster?.fullsize ?: entry.posterFullsizeUrl,
                    posterMegaUrl = poster?.mega ?: entry.posterMegaUrl,
                    isFavorite = true,
                    favoriteUpdatedAt = System.currentTimeMillis(),
                ) ?: LibraryEntry(
                    animeId = animeId,
                    title = title,
                    posterSmallUrl = poster?.small,
                    posterMediumUrl = poster?.medium,
                    posterBigUrl = poster?.big,
                    posterFullsizeUrl = poster?.fullsize,
                    posterMegaUrl = poster?.mega,
                    listId = FAVORITE_ONLY_LIST_ID,
                    isFavorite = true,
                )
            )
            return
        }

        if (entry == null) return
        if (entry.listId >= 0) {
            dao.add(
                entry.copy(
                    isFavorite = false,
                    favoriteUpdatedAt = System.currentTimeMillis(),
                )
            )
        } else {
            dao.delete(animeId)
        }
    }
}
