package su.afk.yummy.tv.domain.library.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.domain.library.model.LibraryItem

interface LibraryRepository {
    fun observeAll(): Flow<List<LibraryItem>>
    suspend fun getAll(): List<LibraryItem>
    fun observeIsInLibrary(animeId: Int): Flow<Boolean>
    fun observeIsFavorite(animeId: Int): Flow<Boolean>
    suspend fun add(item: LibraryItem)
    suspend fun setUserRating(animeId: Int, rating: Int?)
    suspend fun remove(animeId: Int)
    suspend fun delete(animeId: Int)
    suspend fun setFavorite(
        animeId: Int,
        title: String,
        poster: AnimePoster?,
        year: Int?,
        favorite: Boolean,
    )

    suspend fun refreshMetadata(
        animeId: Int,
        title: String,
        poster: AnimePoster?,
        year: Int?,
    )

    suspend fun hasSyncState(userId: Int): Boolean
    suspend fun markSynced(userId: Int)
}
