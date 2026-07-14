package su.afk.yummy.tv.data.library.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.data.library.storage.mapper.toLibraryEntry
import su.afk.yummy.tv.data.library.storage.mapper.toLibraryItem
import su.afk.yummy.tv.data.library.storage.mapper.toStoragePoster
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.domain.library.repository.LibraryRepository

class DefaultLibraryRepository(
    private val store: LibraryStore,
) : LibraryRepository {
    override fun observeAll(): Flow<List<LibraryItem>> =
        store.observeAll()
            .map { entries -> entries.map { it.toLibraryItem() } }
            .distinctUntilChanged()

    override suspend fun getAll(): List<LibraryItem> =
        store.getAll().map { it.toLibraryItem() }

    override fun observeIsInLibrary(animeId: Int): Flow<Boolean> =
        store.observeIsInLibrary(animeId)

    override fun observeIsFavorite(animeId: Int): Flow<Boolean> =
        store.observeIsFavorite(animeId)

    override suspend fun add(item: LibraryItem) {
        store.add(item.toLibraryEntry())
    }

    override suspend fun remove(animeId: Int) {
        store.remove(animeId)
    }

    override suspend fun delete(animeId: Int) {
        store.delete(animeId)
    }

    override suspend fun setFavorite(
        animeId: Int,
        title: String,
        poster: LibraryPoster?,
        favorite: Boolean,
    ) {
        store.setFavorite(
            animeId = animeId,
            title = title,
            poster = poster.toStoragePoster(),
            favorite = favorite,
        )
    }

    override suspend fun refreshMetadata(
        animeId: Int,
        title: String,
        poster: LibraryPoster?,
    ) {
        store.refreshMetadata(
            animeId = animeId,
            title = title,
            poster = poster.toStoragePoster(),
        )
    }

    override suspend fun hasSyncState(userId: Int): Boolean =
        store.hasSyncState(userId)

    override suspend fun markSynced(userId: Int) {
        store.markSynced(userId)
    }
}
