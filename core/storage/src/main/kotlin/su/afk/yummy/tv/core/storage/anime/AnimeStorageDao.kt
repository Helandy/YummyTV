package su.afk.yummy.tv.core.storage.anime

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AnimeStorageDao {

    @Query("SELECT * FROM anime_details WHERE animeId = :animeId AND language = :language LIMIT 1")
    abstract suspend fun getDetailsEntry(animeId: Int, language: String): AnimeDetailsEntry?

    @Query(
        """
        SELECT * FROM anime_detail_titles
        WHERE animeId = :animeId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getOtherTitles(animeId: Int, language: String): List<AnimeDetailTitleEntry>

    @Query(
        """
        SELECT * FROM anime_detail_named_items
        WHERE animeId = :animeId AND language = :language AND kind = :kind
        ORDER BY position
        """
    )
    abstract suspend fun getNamedItems(
        animeId: Int,
        language: String,
        kind: String,
    ): List<AnimeDetailNamedEntry>

    @Query(
        """
        SELECT * FROM anime_viewing_order
        WHERE animeId = :animeId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getViewingOrder(
        animeId: Int,
        language: String
    ): List<AnimeViewingOrderEntry>

    @Query(
        """
        SELECT * FROM anime_screenshots
        WHERE animeId = :animeId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getScreenshots(animeId: Int, language: String): List<AnimeScreenshotEntry>

    @Query("SELECT * FROM anime_video_caches WHERE animeId = :animeId AND language = :language LIMIT 1")
    abstract suspend fun getVideoCacheEntry(animeId: Int, language: String): AnimeVideoCacheEntry?

    @Query(
        """
        SELECT * FROM anime_videos
        WHERE animeId = :animeId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getVideoEntries(animeId: Int, language: String): List<AnimeVideoEntry>

    @Query(
        """
        SELECT * FROM anime_recommendation_caches
        WHERE animeId = :animeId AND language = :language AND fromAi = :fromAi
        LIMIT 1
        """
    )
    abstract suspend fun getRecommendationCacheEntry(
        animeId: Int,
        language: String,
        fromAi: Boolean,
    ): AnimeRecommendationCacheEntry?

    @Query(
        """
        SELECT * FROM anime_recommendations
        WHERE animeId = :animeId AND language = :language AND fromAi = :fromAi
        ORDER BY position
        """
    )
    abstract suspend fun getRecommendationEntries(
        animeId: Int,
        language: String,
        fromAi: Boolean,
    ): List<AnimeRecommendationEntry>

    @Query("SELECT * FROM anime_trailer_caches WHERE animeId = :animeId AND language = :language LIMIT 1")
    abstract suspend fun getTrailerCacheEntry(
        animeId: Int,
        language: String
    ): AnimeTrailerCacheEntry?

    @Query(
        """
        SELECT * FROM anime_trailers
        WHERE animeId = :animeId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getTrailerEntries(animeId: Int, language: String): List<AnimeTrailerEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDetails(entry: AnimeDetailsEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOtherTitles(entries: List<AnimeDetailTitleEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNamedItems(entries: List<AnimeDetailNamedEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertViewingOrder(entries: List<AnimeViewingOrderEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertScreenshots(entries: List<AnimeScreenshotEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertVideoCache(entry: AnimeVideoCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertVideos(entries: List<AnimeVideoEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRecommendationCache(entry: AnimeRecommendationCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRecommendations(entries: List<AnimeRecommendationEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTrailerCache(entry: AnimeTrailerCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTrailers(entries: List<AnimeTrailerEntry>)

    @Query("DELETE FROM anime_details WHERE animeId = :animeId AND language = :language")
    abstract suspend fun deleteDetailsEntry(animeId: Int, language: String)

    @Query("UPDATE anime_details SET cachedAt = 0")
    abstract suspend fun expireAllDetails()

    @Query("DELETE FROM anime_detail_titles WHERE animeId = :animeId AND language = :language")
    abstract suspend fun deleteOtherTitles(animeId: Int, language: String)

    @Query("DELETE FROM anime_detail_named_items WHERE animeId = :animeId AND language = :language")
    abstract suspend fun deleteNamedItems(animeId: Int, language: String)

    @Query("DELETE FROM anime_viewing_order WHERE animeId = :animeId AND language = :language")
    abstract suspend fun deleteViewingOrder(animeId: Int, language: String)

    @Query("DELETE FROM anime_screenshots WHERE animeId = :animeId AND language = :language")
    abstract suspend fun deleteScreenshots(animeId: Int, language: String)

    @Query("DELETE FROM anime_video_caches WHERE animeId = :animeId AND language = :language")
    abstract suspend fun deleteVideoCache(animeId: Int, language: String)

    @Query("DELETE FROM anime_videos WHERE animeId = :animeId AND language = :language")
    abstract suspend fun deleteVideos(animeId: Int, language: String)

    @Query(
        """
        DELETE FROM anime_recommendation_caches
        WHERE animeId = :animeId AND language = :language AND fromAi = :fromAi
        """
    )
    abstract suspend fun deleteRecommendationCache(animeId: Int, language: String, fromAi: Boolean)

    @Query(
        """
        DELETE FROM anime_recommendations
        WHERE animeId = :animeId AND language = :language AND fromAi = :fromAi
        """
    )
    abstract suspend fun deleteRecommendations(animeId: Int, language: String, fromAi: Boolean)

    @Query("DELETE FROM anime_trailer_caches WHERE animeId = :animeId AND language = :language")
    abstract suspend fun deleteTrailerCache(animeId: Int, language: String)

    @Query("DELETE FROM anime_trailers WHERE animeId = :animeId AND language = :language")
    abstract suspend fun deleteTrailers(animeId: Int, language: String)

    @Transaction
    open suspend fun getDetails(animeId: Int, language: String): AnimeDetailsCache? {
        val entry = getDetailsEntry(animeId, language) ?: return null
        return AnimeDetailsCache(
            entry = entry,
            otherTitles = getOtherTitles(animeId, language),
            genres = getNamedItems(animeId, language, ANIME_DETAIL_NAMED_KIND_GENRE),
            creators = getNamedItems(animeId, language, ANIME_DETAIL_NAMED_KIND_CREATOR),
            studios = getNamedItems(animeId, language, ANIME_DETAIL_NAMED_KIND_STUDIO),
            viewingOrder = getViewingOrder(animeId, language),
            screenshots = getScreenshots(animeId, language),
        )
    }

    @Transaction
    open suspend fun getVideos(animeId: Int, language: String): AnimeVideosCache? {
        val entry = getVideoCacheEntry(animeId, language) ?: return null
        return AnimeVideosCache(
            entry = entry,
            videos = getVideoEntries(animeId, language),
        )
    }

    @Transaction
    open suspend fun getRecommendations(
        animeId: Int,
        language: String,
        fromAi: Boolean,
    ): AnimeRecommendationsCache? {
        val entry = getRecommendationCacheEntry(animeId, language, fromAi) ?: return null
        return AnimeRecommendationsCache(
            entry = entry,
            recommendations = getRecommendationEntries(animeId, language, fromAi),
        )
    }

    @Transaction
    open suspend fun getTrailers(animeId: Int, language: String): AnimeTrailersCache? {
        val entry = getTrailerCacheEntry(animeId, language) ?: return null
        return AnimeTrailersCache(
            entry = entry,
            trailers = getTrailerEntries(animeId, language),
        )
    }

    @Transaction
    open suspend fun replaceDetails(cache: AnimeDetailsCache) {
        val animeId = cache.entry.animeId
        val language = cache.entry.language
        deleteDetailsEntry(animeId, language)
        deleteOtherTitles(animeId, language)
        deleteNamedItems(animeId, language)
        deleteViewingOrder(animeId, language)
        deleteScreenshots(animeId, language)

        insertDetails(cache.entry)
        if (cache.otherTitles.isNotEmpty()) insertOtherTitles(cache.otherTitles)

        val namedItems = cache.genres + cache.creators + cache.studios
        if (namedItems.isNotEmpty()) insertNamedItems(namedItems)

        if (cache.viewingOrder.isNotEmpty()) insertViewingOrder(cache.viewingOrder)
        if (cache.screenshots.isNotEmpty()) insertScreenshots(cache.screenshots)
    }

    @Transaction
    open suspend fun deleteDetails(animeId: Int, language: String) {
        deleteDetailsEntry(animeId, language)
        deleteOtherTitles(animeId, language)
        deleteNamedItems(animeId, language)
        deleteViewingOrder(animeId, language)
        deleteScreenshots(animeId, language)
    }

    @Transaction
    open suspend fun replaceVideos(cache: AnimeVideosCache) {
        val animeId = cache.entry.animeId
        val language = cache.entry.language
        deleteVideoCache(animeId, language)
        deleteVideos(animeId, language)

        insertVideoCache(cache.entry)
        if (cache.videos.isNotEmpty()) insertVideos(cache.videos)
    }

    @Transaction
    open suspend fun replaceRecommendations(cache: AnimeRecommendationsCache) {
        val animeId = cache.entry.animeId
        val language = cache.entry.language
        val fromAi = cache.entry.fromAi
        deleteRecommendationCache(animeId, language, fromAi)
        deleteRecommendations(animeId, language, fromAi)

        insertRecommendationCache(cache.entry)
        if (cache.recommendations.isNotEmpty()) insertRecommendations(cache.recommendations)
    }

    @Transaction
    open suspend fun replaceTrailers(cache: AnimeTrailersCache) {
        val animeId = cache.entry.animeId
        val language = cache.entry.language
        deleteTrailerCache(animeId, language)
        deleteTrailers(animeId, language)

        insertTrailerCache(cache.entry)
        if (cache.trailers.isNotEmpty()) insertTrailers(cache.trailers)
    }
}
