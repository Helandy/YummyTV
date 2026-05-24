package su.afk.yummy.tv.core.storage.library

import kotlinx.coroutines.flow.Flow

class LibraryStore(private val dao: LibraryDao) {
    fun observeAll(): Flow<List<LibraryEntry>> = dao.observeAll()
    suspend fun getAll(): List<LibraryEntry> = dao.getAll()
    fun observeIsInLibrary(animeId: Int): Flow<Boolean> = dao.observeIsInLibrary(animeId)
    suspend fun add(entry: LibraryEntry) = dao.add(entry)
    suspend fun remove(animeId: Int) = dao.remove(animeId)
}
