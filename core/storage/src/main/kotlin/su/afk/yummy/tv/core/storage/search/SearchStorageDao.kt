package su.afk.yummy.tv.core.storage.search

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class SearchStorageDao {

    @Query("SELECT * FROM search_pages WHERE pageKey = :pageKey LIMIT 1")
    abstract suspend fun getPageEntry(pageKey: String): SearchPageEntry?

    @Query("SELECT * FROM search_items WHERE pageKey = :pageKey ORDER BY position")
    abstract suspend fun getPageItems(pageKey: String): List<SearchItemEntry>

    @Query("SELECT * FROM search_filter_options WHERE language = :language LIMIT 1")
    abstract suspend fun getFilterOptionsEntry(language: String): SearchFilterOptionsEntry?

    @Query("SELECT * FROM search_genre_groups WHERE language = :language ORDER BY position")
    abstract suspend fun getGenreGroups(language: String): List<SearchGenreGroupEntry>

    @Query("SELECT * FROM search_genres WHERE language = :language ORDER BY position")
    abstract suspend fun getGenres(language: String): List<SearchGenreEntry>

    @Query("SELECT * FROM search_types WHERE language = :language ORDER BY position")
    abstract suspend fun getTypes(language: String): List<SearchTypeEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPage(entry: SearchPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPageItems(entries: List<SearchItemEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertFilterOptions(entry: SearchFilterOptionsEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertGenreGroups(entries: List<SearchGenreGroupEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertGenres(entries: List<SearchGenreEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTypes(entries: List<SearchTypeEntry>)

    @Query("DELETE FROM search_pages WHERE pageKey = :pageKey")
    abstract suspend fun deletePage(pageKey: String)

    @Query("DELETE FROM search_items WHERE pageKey = :pageKey")
    abstract suspend fun deletePageItems(pageKey: String)

    @Query("DELETE FROM search_filter_options WHERE language = :language")
    abstract suspend fun deleteFilterOptions(language: String)

    @Query("DELETE FROM search_genre_groups WHERE language = :language")
    abstract suspend fun deleteGenreGroups(language: String)

    @Query("DELETE FROM search_genres WHERE language = :language")
    abstract suspend fun deleteGenres(language: String)

    @Query("DELETE FROM search_types WHERE language = :language")
    abstract suspend fun deleteTypes(language: String)

    @Transaction
    open suspend fun getPage(pageKey: String): SearchPageCache? {
        val entry = getPageEntry(pageKey) ?: return null
        return SearchPageCache(
            entry = entry,
            items = getPageItems(pageKey),
        )
    }

    @Transaction
    open suspend fun getFilterOptions(language: String): SearchFilterOptionsCache? {
        val entry = getFilterOptionsEntry(language) ?: return null
        return SearchFilterOptionsCache(
            entry = entry,
            genreGroups = getGenreGroups(language),
            genres = getGenres(language),
            types = getTypes(language),
        )
    }

    @Transaction
    open suspend fun replacePage(cache: SearchPageCache) {
        val pageKey = cache.entry.pageKey
        deletePage(pageKey)
        deletePageItems(pageKey)

        insertPage(cache.entry)
        if (cache.items.isNotEmpty()) insertPageItems(cache.items)
    }

    @Transaction
    open suspend fun replaceFilterOptions(cache: SearchFilterOptionsCache) {
        val language = cache.entry.language
        deleteFilterOptions(language)
        deleteGenreGroups(language)
        deleteGenres(language)
        deleteTypes(language)

        insertFilterOptions(cache.entry)
        if (cache.genreGroups.isNotEmpty()) insertGenreGroups(cache.genreGroups)
        if (cache.genres.isNotEmpty()) insertGenres(cache.genres)
        if (cache.types.isNotEmpty()) insertTypes(cache.types)
    }
}
