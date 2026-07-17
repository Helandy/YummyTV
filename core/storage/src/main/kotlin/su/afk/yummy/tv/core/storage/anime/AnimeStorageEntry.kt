package su.afk.yummy.tv.core.storage.anime

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

const val ANIME_DETAIL_NAMED_KIND_GENRE = "genre"
const val ANIME_DETAIL_NAMED_KIND_CREATOR = "creator"
const val ANIME_DETAIL_NAMED_KIND_STUDIO = "studio"

@Entity(
    tableName = "anime_details",
    primaryKeys = ["animeId", "language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_anime_details_cachedAt"),
    ],
)
data class AnimeDetailsEntry(
    val animeId: Int,
    val language: String,
    val animeUrl: String,
    val title: String,
    val description: String,
    val posterSmallUrl: String? = null,
    val posterMediumUrl: String? = null,
    val posterBigUrl: String? = null,
    val posterFullsizeUrl: String? = null,
    val posterMegaUrl: String? = null,
    val ratingAverage: Double? = null,
    val ratingCounters: Int? = null,
    val ratingKinopoisk: Double? = null,
    val ratingShikimori: Double? = null,
    val ratingMyAnimeList: Double? = null,
    val year: Int? = null,
    val ageRating: String? = null,
    val views: Int? = null,
    val status: String? = null,
    val type: String? = null,
    val episodesCount: Int? = null,
    val episodesAired: Int? = null,
    val episodesNextDateEpochSeconds: Long? = null,
    val episodesPrevDateEpochSeconds: Long? = null,
    @ColumnInfo(defaultValue = "0") val reviewsCount: Int = 0,
    val cachedAt: Long,
)

@Entity(
    tableName = "anime_detail_titles",
    primaryKeys = ["animeId", "language", "position"],
    indices = [
        Index(
            value = ["animeId", "language"],
            name = "index_anime_detail_titles_animeId_language",
        ),
    ],
)
data class AnimeDetailTitleEntry(
    val animeId: Int,
    val language: String,
    val position: Int,
    val title: String,
)

@Entity(
    tableName = "anime_detail_named_items",
    primaryKeys = ["animeId", "language", "kind", "position"],
    indices = [
        Index(
            value = ["animeId", "language", "kind"],
            name = "index_anime_detail_named_items_animeId_language_kind",
        ),
    ],
)
data class AnimeDetailNamedEntry(
    val animeId: Int,
    val language: String,
    val kind: String,
    val position: Int,
    val itemId: Int? = null,
    val title: String,
    val itemUrl: String? = null,
)

@Entity(
    tableName = "anime_viewing_order",
    primaryKeys = ["animeId", "language", "position"],
    indices = [
        Index(
            value = ["animeId", "language"],
            name = "index_anime_viewing_order_animeId_language",
        ),
    ],
)
data class AnimeViewingOrderEntry(
    val animeId: Int,
    val language: String,
    val position: Int,
    val relatedAnimeId: Int,
    val title: String,
    val relation: String? = null,
    val type: String? = null,
    val episodesCount: Int? = null,
    val posterSmallUrl: String? = null,
    val posterMediumUrl: String? = null,
    val posterBigUrl: String? = null,
    val posterFullsizeUrl: String? = null,
    val posterMegaUrl: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
)

@Entity(
    tableName = "anime_screenshots",
    primaryKeys = ["animeId", "language", "position"],
    indices = [
        Index(
            value = ["animeId", "language"],
            name = "index_anime_screenshots_animeId_language",
        ),
    ],
)
data class AnimeScreenshotEntry(
    val animeId: Int,
    val language: String,
    val position: Int,
    val screenshotId: Int? = null,
    val episode: String? = null,
    val smallUrl: String? = null,
    val fullUrl: String? = null,
)

@Entity(
    tableName = "anime_video_caches",
    primaryKeys = ["animeId", "language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_anime_video_caches_cachedAt"),
    ],
)
data class AnimeVideoCacheEntry(
    val animeId: Int,
    val language: String,
    val cachedAt: Long,
)

@Entity(
    tableName = "anime_videos",
    primaryKeys = ["animeId", "language", "position"],
    indices = [
        Index(value = ["animeId", "language"], name = "index_anime_videos_animeId_language"),
    ],
)
data class AnimeVideoEntry(
    val animeId: Int,
    val language: String,
    val position: Int,
    val videoId: Int,
    val episode: String,
    val dubbing: String,
    val player: String,
    val playerId: Int? = null,
    val iframeUrl: String,
    val durationSeconds: Int? = null,
    val views: Int? = null,
    val watchedEndTimeSeconds: Int? = null,
    val watchedDateSeconds: Long? = null,
    val openingStartMs: Long? = null,
    val openingEndMs: Long? = null,
    val endingStartMs: Long? = null,
    val endingEndMs: Long? = null,
)

@Entity(
    tableName = "anime_recommendation_caches",
    primaryKeys = ["animeId", "language", "fromAi"],
    indices = [
        Index(value = ["cachedAt"], name = "index_anime_recommendation_caches_cachedAt"),
    ],
)
data class AnimeRecommendationCacheEntry(
    val animeId: Int,
    val language: String,
    val fromAi: Boolean,
    val cachedAt: Long,
)

@Entity(
    tableName = "anime_recommendations",
    primaryKeys = ["animeId", "language", "fromAi", "position"],
    indices = [
        Index(
            value = ["animeId", "language", "fromAi"],
            name = "index_anime_recommendations_animeId_language_fromAi",
        ),
    ],
)
data class AnimeRecommendationEntry(
    val animeId: Int,
    val language: String,
    val fromAi: Boolean,
    val position: Int,
    val recommendationAnimeId: Int,
    val title: String,
    val posterSmallUrl: String? = null,
    val posterMediumUrl: String? = null,
    val posterBigUrl: String? = null,
    val posterFullsizeUrl: String? = null,
    val posterMegaUrl: String? = null,
    val rating: Double? = null,
    val type: String? = null,
    val year: Int? = null,
)

@Entity(
    tableName = "anime_trailer_caches",
    primaryKeys = ["animeId", "language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_anime_trailer_caches_cachedAt"),
    ],
)
data class AnimeTrailerCacheEntry(
    val animeId: Int,
    val language: String,
    val cachedAt: Long,
)

@Entity(
    tableName = "anime_trailers",
    primaryKeys = ["animeId", "language", "position"],
    indices = [
        Index(value = ["animeId", "language"], name = "index_anime_trailers_animeId_language"),
    ],
)
data class AnimeTrailerEntry(
    val animeId: Int,
    val language: String,
    val position: Int,
    val iframeUrl: String,
)
