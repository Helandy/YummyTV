package su.afk.yummy.tv.core.storage.library

import kotlinx.coroutines.flow.Flow

/** Абстракция над локальной библиотекой/избранным пользователя — позволяет подменять реализацию в тестах. */
interface LibraryStorage {

    fun observeAll(): Flow<List<LibraryEntry>>

    suspend fun getAll(): List<LibraryEntry>

    fun observeIsInLibrary(animeId: Int): Flow<Boolean>

    fun observeIsFavorite(animeId: Int): Flow<Boolean>

    suspend fun add(entry: LibraryEntry)

    suspend fun hasSyncState(userId: Int): Boolean

    suspend fun markSynced(userId: Int)

    suspend fun refreshMetadata(
        animeId: Int,
        title: String,
        poster: LibraryPoster?,
        year: Int?,
    )

    /** Обновляет оценку у тайтла, уже лежащего в библиотеке; `null` — оценка снята. */
    suspend fun setUserRating(animeId: Int, rating: Int?)

    suspend fun remove(animeId: Int)

    suspend fun delete(animeId: Int)

    suspend fun setFavorite(
        animeId: Int,
        title: String,
        poster: LibraryPoster?,
        year: Int?,
        favorite: Boolean,
    )
}
