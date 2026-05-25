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
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.core.storage.db.AppDatabase
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.core.storage.settings.SettingsStore
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "yummy_cache.db")
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideCacheStore(db: AppDatabase): CacheStore = CacheStore(db.cacheDao())

    @Provides
    @Singleton
    fun provideLibraryStore(db: AppDatabase): LibraryStore = LibraryStore(db.libraryDao())

    @Provides
    @Singleton
    fun provideWatchProgressStore(db: AppDatabase): WatchProgressStore = WatchProgressStore(db.watchProgressDao())

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore = SettingsStore(context)
}
