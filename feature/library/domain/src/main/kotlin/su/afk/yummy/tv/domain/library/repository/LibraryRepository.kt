package su.afk.yummy.tv.domain.library.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster

interface LibraryRepository {
    fun observeAll(): Flow<List<LibraryItem>>
    suspend fun getAll(): List<LibraryItem>
    fun observeIsInLibrary(animeId: Int): Flow<Boolean>
    fun observeIsFavorite(animeId: Int): Flow<Boolean>
    suspend fun add(item: LibraryItem)
    suspend fun remove(animeId: Int)
    suspend fun delete(animeId: Int)
    suspend fun setFavorite(
        animeId: Int,
        title: String,
        poster: LibraryPoster?,
        favorite: Boolean,
    )

    suspend fun refreshMetadata(
        animeId: Int,
        title: String,
        poster: LibraryPoster?,
    )

    suspend fun hasSyncState(userId: Int): Boolean
    suspend fun markSynced(userId: Int)
}
