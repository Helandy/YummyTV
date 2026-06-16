package su.afk.yummy.tv.core.storage.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.anime.AnimeStorageStore
import su.afk.yummy.tv.core.storage.collection.CollectionStorageStore
import su.afk.yummy.tv.core.storage.db.AppDatabase
import su.afk.yummy.tv.core.storage.home.HomeFeedStore
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleStore
import su.afk.yummy.tv.core.storage.search.SearchStorageStore
import su.afk.yummy.tv.core.storage.top.AnimeTopStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE watch_progress ADD COLUMN videoId INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE library ADD COLUMN listId INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE library ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS library_new (
                    animeId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    posterSmallUrl TEXT,
                    posterMediumUrl TEXT,
                    posterBigUrl TEXT,
                    posterFullsizeUrl TEXT,
                    posterMegaUrl TEXT,
                    addedAt INTEGER NOT NULL,
                    listId INTEGER NOT NULL,
                    isFavorite INTEGER NOT NULL,
                    PRIMARY KEY(animeId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO library_new (
                    animeId,
                    title,
                    posterSmallUrl,
                    posterMediumUrl,
                    posterBigUrl,
                    posterFullsizeUrl,
                    posterMegaUrl,
                    addedAt,
                    listId,
                    isFavorite
                )
                SELECT
                    animeId,
                    title,
                    CASE WHEN posterUrl IS NULL THEN NULL ELSE replace(replace(replace(replace(replace(replace(replace(posterUrl, '/medium/', '/small/'), '/big/', '/small/'), '/full/', '/small/'), '/huge/', '/small/'), '/mega/', '/small/'), '.jpg', '.webp'), '.avif', '.webp') END,
                    CASE WHEN posterUrl IS NULL THEN NULL ELSE replace(replace(replace(replace(replace(replace(replace(posterUrl, '/small/', '/medium/'), '/big/', '/medium/'), '/full/', '/medium/'), '/huge/', '/medium/'), '/mega/', '/medium/'), '.jpg', '.webp'), '.avif', '.webp') END,
                    CASE WHEN posterUrl IS NULL THEN NULL ELSE replace(replace(replace(replace(replace(replace(replace(posterUrl, '/small/', '/big/'), '/medium/', '/big/'), '/full/', '/big/'), '/huge/', '/big/'), '/mega/', '/big/'), '.jpg', '.webp'), '.avif', '.webp') END,
                    CASE WHEN posterUrl IS NULL THEN NULL ELSE replace(replace(replace(replace(replace(replace(replace(posterUrl, '/small/', '/full/'), '/medium/', '/full/'), '/big/', '/full/'), '/huge/', '/full/'), '/mega/', '/full/'), '.webp', '.jpg'), '.avif', '.jpg') END,
                    CASE WHEN posterUrl IS NULL THEN NULL ELSE replace(replace(replace(replace(replace(replace(replace(posterUrl, '/small/', '/mega/'), '/medium/', '/mega/'), '/big/', '/mega/'), '/full/', '/mega/'), '/huge/', '/mega/'), '.webp', '.avif'), '.jpg', '.avif') END,
                    addedAt,
                    listId,
                    isFavorite
                FROM library
                """.trimIndent()
            )
            db.execSQL("DROP TABLE library")
            db.execSQL("ALTER TABLE library_new RENAME TO library")
        }
    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_watch_progress_updatedAt ON watch_progress(updatedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_library_addedAt ON library(addedAt)")
        }
    }

    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_details (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    animeUrl TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    posterSmallUrl TEXT,
                    posterMediumUrl TEXT,
                    posterBigUrl TEXT,
                    posterFullsizeUrl TEXT,
                    posterMegaUrl TEXT,
                    ratingAverage REAL,
                    ratingCounters INTEGER,
                    ratingKinopoisk REAL,
                    ratingShikimori REAL,
                    ratingMyAnimeList REAL,
                    year INTEGER,
                    ageRating TEXT,
                    views INTEGER,
                    status TEXT,
                    type TEXT,
                    episodesCount INTEGER,
                    episodesAired INTEGER,
                    episodesNextDateEpochSeconds INTEGER,
                    episodesPrevDateEpochSeconds INTEGER,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(animeId, language)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_anime_details_cachedAt ON anime_details(cachedAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_detail_titles (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    PRIMARY KEY(animeId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_detail_titles_animeId_language
                ON anime_detail_titles(animeId, language)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_detail_named_items (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    itemId INTEGER,
                    title TEXT NOT NULL,
                    PRIMARY KEY(animeId, language, kind, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_detail_named_items_animeId_language_kind
                ON anime_detail_named_items(animeId, language, kind)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_viewing_order (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    relatedAnimeId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    relation TEXT,
                    type TEXT,
                    episodesCount INTEGER,
                    posterSmallUrl TEXT,
                    posterMediumUrl TEXT,
                    posterBigUrl TEXT,
                    posterFullsizeUrl TEXT,
                    posterMegaUrl TEXT,
                    year INTEGER,
                    rating REAL,
                    PRIMARY KEY(animeId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_viewing_order_animeId_language
                ON anime_viewing_order(animeId, language)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_screenshots (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    screenshotId INTEGER,
                    episode TEXT,
                    smallUrl TEXT,
                    fullUrl TEXT,
                    PRIMARY KEY(animeId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_screenshots_animeId_language
                ON anime_screenshots(animeId, language)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_video_caches (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(animeId, language)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_anime_video_caches_cachedAt ON anime_video_caches(cachedAt)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_videos (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    videoId INTEGER NOT NULL,
                    episode TEXT NOT NULL,
                    dubbing TEXT NOT NULL,
                    player TEXT NOT NULL,
                    playerId INTEGER,
                    iframeUrl TEXT NOT NULL,
                    durationSeconds INTEGER,
                    views INTEGER,
                    openingStartMs INTEGER,
                    openingEndMs INTEGER,
                    endingStartMs INTEGER,
                    endingEndMs INTEGER,
                    PRIMARY KEY(animeId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_videos_animeId_language
                ON anime_videos(animeId, language)
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_recommendation_caches (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    fromAi INTEGER NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(animeId, language, fromAi)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_recommendation_caches_cachedAt
                ON anime_recommendation_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_recommendations (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    fromAi INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    recommendationAnimeId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    posterSmallUrl TEXT,
                    posterMediumUrl TEXT,
                    posterBigUrl TEXT,
                    posterFullsizeUrl TEXT,
                    posterMegaUrl TEXT,
                    rating REAL,
                    type TEXT,
                    year INTEGER,
                    PRIMARY KEY(animeId, language, fromAi, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_recommendations_animeId_language_fromAi
                ON anime_recommendations(animeId, language, fromAi)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_trailer_caches (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(animeId, language)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_trailer_caches_cachedAt
                ON anime_trailer_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_trailers (
                    animeId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    iframeUrl TEXT NOT NULL,
                    PRIMARY KEY(animeId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_trailers_animeId_language
                ON anime_trailers(animeId, language)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS home_feed_caches (
                    language TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(language)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_home_feed_caches_cachedAt
                ON home_feed_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS home_feed_items (
                    language TEXT NOT NULL,
                    container TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    itemId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    posterSmallUrl TEXT,
                    posterMediumUrl TEXT,
                    posterBigUrl TEXT,
                    posterFullsizeUrl TEXT,
                    posterMegaUrl TEXT,
                    rating REAL,
                    actionType TEXT NOT NULL,
                    actionId INTEGER NOT NULL,
                    PRIMARY KEY(language, container, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_home_feed_items_language_container
                ON home_feed_items(language, container)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_top_pages (
                    type TEXT NOT NULL,
                    language TEXT NOT NULL,
                    `limit` INTEGER NOT NULL,
                    `offset` INTEGER NOT NULL,
                    responseSize INTEGER NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(type, language, `limit`, `offset`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_top_pages_cachedAt
                ON anime_top_pages(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_top_items (
                    type TEXT NOT NULL,
                    language TEXT NOT NULL,
                    `limit` INTEGER NOT NULL,
                    `offset` INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    animeId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    posterUrl TEXT,
                    rating REAL,
                    PRIMARY KEY(type, language, `limit`, `offset`, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_top_items_page
                ON anime_top_items(type, language, `limit`, `offset`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_schedule_caches (
                    language TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(language)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_schedule_caches_cachedAt
                ON anime_schedule_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS anime_schedule_items (
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    animeId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    posterUrl TEXT,
                    nextDateEpochSeconds INTEGER,
                    previousDateEpochSeconds INTEGER,
                    airedEpisodes INTEGER,
                    totalEpisodes INTEGER,
                    PRIMARY KEY(language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_anime_schedule_items_language
                ON anime_schedule_items(language)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS search_pages (
                    pageKey TEXT NOT NULL,
                    language TEXT NOT NULL,
                    `limit` INTEGER NOT NULL,
                    `offset` INTEGER NOT NULL,
                    responseSize INTEGER NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(pageKey)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_search_pages_language ON search_pages(language)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_search_pages_cachedAt ON search_pages(cachedAt)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS search_items (
                    pageKey TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    animeId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    posterUrl TEXT,
                    rating REAL,
                    PRIMARY KEY(pageKey, position)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_search_items_pageKey ON search_items(pageKey)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS search_filter_options (
                    language TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(language)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_search_filter_options_cachedAt
                ON search_filter_options(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS search_genre_groups (
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    groupId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    PRIMARY KEY(language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_search_genre_groups_language
                ON search_genre_groups(language)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS search_genres (
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    genreId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    groupId INTEGER NOT NULL,
                    PRIMARY KEY(language, position)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_search_genres_language ON search_genres(language)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS search_types (
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    typeId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    PRIMARY KEY(language, position)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_search_types_language ON search_types(language)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS collection_details (
                    collectionId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    views INTEGER NOT NULL,
                    posterUrl TEXT,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(collectionId, language)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_collection_details_cachedAt
                ON collection_details(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS collection_anime_items (
                    collectionId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    animeId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    posterUrl TEXT,
                    rating REAL,
                    PRIMARY KEY(collectionId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_collection_anime_items_collectionId_language
                ON collection_anime_items(collectionId, language)
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_profiles (
                    profileKey TEXT NOT NULL,
                    userId INTEGER NOT NULL,
                    nickname TEXT NOT NULL,
                    avatarUrl TEXT,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(profileKey)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_account_profiles_userId ON account_profiles(userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_account_profiles_cachedAt ON account_profiles(cachedAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_list_pages (
                    userId INTEGER NOT NULL,
                    listId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId, listId, language)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_list_pages_cachedAt
                ON account_user_list_pages(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_list_items (
                    userId INTEGER NOT NULL,
                    listId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    animeId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    posterUrl TEXT,
                    posterSmallUrl TEXT,
                    posterMediumUrl TEXT,
                    posterBigUrl TEXT,
                    posterFullsizeUrl TEXT,
                    posterMegaUrl TEXT,
                    rating REAL,
                    year INTEGER,
                    userListId INTEGER,
                    isFavorite INTEGER NOT NULL,
                    PRIMARY KEY(userId, listId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_list_items_page
                ON account_user_list_items(userId, listId, language)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_anime_list_states (
                    userId INTEGER NOT NULL,
                    animeId INTEGER NOT NULL,
                    listId INTEGER,
                    isFavorite INTEGER NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId, animeId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_anime_list_states_cachedAt
                ON account_anime_list_states(cachedAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_rating_bucket_caches (
                    animeId INTEGER NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(animeId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_rating_bucket_caches_cachedAt
                ON account_rating_bucket_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_rating_buckets (
                    animeId INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    rating INTEGER NOT NULL,
                    count INTEGER NOT NULL,
                    PRIMARY KEY(animeId, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_rating_buckets_animeId
                ON account_rating_buckets(animeId)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_ratings (
                    userId INTEGER NOT NULL,
                    animeId INTEGER NOT NULL,
                    rating INTEGER,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId, animeId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_ratings_cachedAt
                ON account_user_ratings(cachedAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_list_stats_caches (
                    animeId INTEGER NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(animeId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_list_stats_caches_cachedAt
                ON account_list_stats_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_list_stats (
                    animeId INTEGER NOT NULL,
                    listId INTEGER NOT NULL,
                    count INTEGER NOT NULL,
                    PRIMARY KEY(animeId, listId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_list_stats_animeId
                ON account_list_stats(animeId)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_collection_pages (
                    pageKey TEXT NOT NULL,
                    language TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(pageKey)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_account_collection_pages_language ON account_collection_pages(language)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_account_collection_pages_cachedAt ON account_collection_pages(cachedAt)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_collection_items (
                    pageKey TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    collectionId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    posterUrl TEXT,
                    posterSmallUrl TEXT,
                    posterMediumUrl TEXT,
                    posterBigUrl TEXT,
                    posterFullsizeUrl TEXT,
                    posterMegaUrl TEXT,
                    views INTEGER,
                    PRIMARY KEY(pageKey, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_account_collection_items_pageKey ON account_collection_items(pageKey)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_video_subscription_caches (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId, language)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_video_subscription_caches_cachedAt
                ON account_video_subscription_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_video_subscriptions (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    animeId INTEGER NOT NULL,
                    animeUrl TEXT NOT NULL,
                    playerId INTEGER,
                    player TEXT NOT NULL,
                    dubbing TEXT NOT NULL,
                    posterUrl TEXT,
                    title TEXT NOT NULL,
                    PRIMARY KEY(userId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_video_subscriptions_userId_language
                ON account_video_subscriptions(userId, language)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_notification_pages (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    `limit` INTEGER NOT NULL,
                    `offset` INTEGER NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId, language, `limit`, `offset`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_notification_pages_cachedAt
                ON account_notification_pages(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_notifications (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    `limit` INTEGER NOT NULL,
                    `offset` INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    notificationId INTEGER NOT NULL,
                    dateSeconds INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    text TEXT NOT NULL,
                    clickUri TEXT NOT NULL,
                    type TEXT NOT NULL,
                    subType TEXT NOT NULL,
                    viewed INTEGER NOT NULL,
                    objectId INTEGER,
                    animeSlug TEXT,
                    isNewEpisode INTEGER NOT NULL,
                    PRIMARY KEY(userId, language, `limit`, `offset`, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_notifications_page
                ON account_notifications(userId, language, `limit`, `offset`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_notification_count_caches (
                    userId INTEGER NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_notification_count_caches_cachedAt
                ON account_notification_count_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_notification_counts (
                    userId INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    count INTEGER NOT NULL,
                    PRIMARY KEY(userId, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_notification_counts_userId
                ON account_notification_counts(userId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_notification_anime (
                    slug TEXT NOT NULL,
                    animeId INTEGER,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(slug)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_notification_anime_cachedAt
                ON account_notification_anime(cachedAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_stats_caches (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId, language)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_stats_caches_cachedAt
                ON account_user_stats_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_genre_stats (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    genreId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    count INTEGER NOT NULL,
                    PRIMARY KEY(userId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_genre_stats_userId_language
                ON account_user_genre_stats(userId, language)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_rating_stats (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    rating INTEGER NOT NULL,
                    count INTEGER NOT NULL,
                    PRIMARY KEY(userId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_rating_stats_userId_language
                ON account_user_rating_stats(userId, language)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_list_watch_stats (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    listId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    href TEXT NOT NULL,
                    seconds INTEGER NOT NULL,
                    PRIMARY KEY(userId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_list_watch_stats_userId_language
                ON account_user_list_watch_stats(userId, language)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_type_stats (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    typeId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    shortName TEXT NOT NULL,
                    count INTEGER NOT NULL,
                    PRIMARY KEY(userId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_type_stats_userId_language
                ON account_user_type_stats(userId, language)
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS cache")
        }
    }

    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_profile_summary_caches (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    nickname TEXT NOT NULL,
                    avatarUrl TEXT,
                    bannerUrl TEXT,
                    registerDateSeconds INTEGER NOT NULL,
                    birthDateSeconds INTEGER NOT NULL,
                    sex INTEGER NOT NULL,
                    about TEXT NOT NULL,
                    daysOnline INTEGER NOT NULL,
                    watchingCount INTEGER NOT NULL,
                    plannedCount INTEGER NOT NULL,
                    completedCount INTEGER NOT NULL,
                    droppedCount INTEGER NOT NULL,
                    postponedCount INTEGER NOT NULL,
                    favoriteCount INTEGER NOT NULL,
                    friendsCount INTEGER NOT NULL,
                    reviewsCount INTEGER NOT NULL,
                    commentsCount INTEGER NOT NULL,
                    postsCount INTEGER NOT NULL,
                    collectionsCount INTEGER NOT NULL,
                    PRIMARY KEY(userId, language)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_profile_summary_caches_cachedAt
                ON account_user_profile_summary_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_profile_watch_types (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    typeId INTEGER NOT NULL,
                    alias TEXT NOT NULL,
                    title TEXT NOT NULL,
                    shortName TEXT NOT NULL,
                    spentSeconds INTEGER NOT NULL,
                    PRIMARY KEY(userId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_profile_watch_types_userId_language
                ON account_user_profile_watch_types(userId, language)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_user_profile_watch_history (
                    userId INTEGER NOT NULL,
                    language TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    dateSeconds INTEGER NOT NULL,
                    durationSeconds INTEGER NOT NULL,
                    episodeCount INTEGER NOT NULL,
                    PRIMARY KEY(userId, language, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_account_user_profile_watch_history_userId_language
                ON account_user_profile_watch_history(userId, language)
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_home_feed_caches_cachedAt")
            db.execSQL("DROP INDEX IF EXISTS index_home_feed_items_language_container")
            db.execSQL("ALTER TABLE home_feed_caches RENAME TO home_feed_caches_old")
            db.execSQL("ALTER TABLE home_feed_items RENAME TO home_feed_items_old")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS home_feed_caches (
                    language TEXT NOT NULL,
                    watchSignature TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(language, watchSignature)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_home_feed_caches_cachedAt
                ON home_feed_caches(cachedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS home_feed_items (
                    language TEXT NOT NULL,
                    watchSignature TEXT NOT NULL,
                    container TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    itemId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    posterSmallUrl TEXT,
                    posterMediumUrl TEXT,
                    posterBigUrl TEXT,
                    posterFullsizeUrl TEXT,
                    posterMegaUrl TEXT,
                    rating REAL,
                    actionType TEXT NOT NULL,
                    actionId INTEGER NOT NULL,
                    PRIMARY KEY(language, watchSignature, container, position)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_home_feed_items_language_signature_container
                ON home_feed_items(language, watchSignature, container)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO home_feed_caches (
                    language,
                    watchSignature,
                    cachedAt
                )
                SELECT
                    language,
                    '',
                    cachedAt
                FROM home_feed_caches_old
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO home_feed_items (
                    language,
                    watchSignature,
                    container,
                    position,
                    itemId,
                    title,
                    description,
                    posterSmallUrl,
                    posterMediumUrl,
                    posterBigUrl,
                    posterFullsizeUrl,
                    posterMegaUrl,
                    rating,
                    actionType,
                    actionId
                )
                SELECT
                    language,
                    '',
                    container,
                    position,
                    itemId,
                    title,
                    description,
                    posterSmallUrl,
                    posterMediumUrl,
                    posterBigUrl,
                    posterFullsizeUrl,
                    posterMegaUrl,
                    rating,
                    actionType,
                    actionId
                FROM home_feed_items_old
                """.trimIndent()
            )
            db.execSQL("DROP TABLE home_feed_caches_old")
            db.execSQL("DROP TABLE home_feed_items_old")
        }
    }

    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE home_feed_items ADD COLUMN episode TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE home_feed_items ADD COLUMN episodeUrl TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE home_feed_items ADD COLUMN positionMs INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE home_feed_items ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE home_feed_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE home_feed_items ADD COLUMN playerName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE home_feed_items ADD COLUMN dubbing TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE home_feed_items ADD COLUMN screenshotUrl TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE library ADD COLUMN listUpdatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE library ADD COLUMN favoriteUpdatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE library SET listUpdatedAt = addedAt WHERE listUpdatedAt = 0")
            db.execSQL("UPDATE library SET favoriteUpdatedAt = addedAt WHERE isFavorite = 1 AND favoriteUpdatedAt = 0")
            db.execSQL("ALTER TABLE account_user_list_items ADD COLUMN updatedAtSeconds INTEGER")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS continue_watching_suppressions (
                    animeId INTEGER NOT NULL,
                    suppressedAt INTEGER NOT NULL,
                    PRIMARY KEY(animeId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_continue_watching_suppressions_suppressedAt
                ON continue_watching_suppressions(suppressedAt)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS library_sync_states (
                    userId INTEGER NOT NULL,
                    syncedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId)
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "yummy_cache.db")
            .addMigrations(
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20,
                MIGRATION_20_21,
            )
            .fallbackToDestructiveMigrationFrom(
                dropAllTables = true,
                1,
                2,
                3,
                4,
                5,
                6,
            )
            .build()

    @Provides
    @Singleton
    fun provideLibraryStore(db: AppDatabase): LibraryStore = LibraryStore(db.libraryDao())

    @Provides
    @Singleton
    fun provideWatchProgressStore(db: AppDatabase): WatchProgressStore = WatchProgressStore(db.watchProgressDao())

    @Provides
    @Singleton
    fun provideAnimeStorageStore(db: AppDatabase): AnimeStorageStore =
        AnimeStorageStore(db.animeStorageDao())

    @Provides
    @Singleton
    fun provideHomeFeedStore(db: AppDatabase): HomeFeedStore = HomeFeedStore(db.homeFeedDao())

    @Provides
    @Singleton
    fun provideAnimeTopStore(db: AppDatabase): AnimeTopStore = AnimeTopStore(db.animeTopDao())

    @Provides
    @Singleton
    fun provideAnimeScheduleStore(db: AppDatabase): AnimeScheduleStore =
        AnimeScheduleStore(db.animeScheduleDao())

    @Provides
    @Singleton
    fun provideSearchStorageStore(db: AppDatabase): SearchStorageStore =
        SearchStorageStore(db.searchStorageDao())

    @Provides
    @Singleton
    fun provideCollectionStorageStore(db: AppDatabase): CollectionStorageStore =
        CollectionStorageStore(db.collectionStorageDao())

    @Provides
    @Singleton
    fun provideAccountStorageStore(db: AppDatabase): AccountStorageStore =
        AccountStorageStore(db.accountStorageDao())
}
