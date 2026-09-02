package su.afk.yummy.tv.core.storage.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_video_downloads_duplicate_key")
        db.execSQL(
            """
            DELETE FROM video_downloads
            WHERE id NOT IN (
                SELECT kept.id
                FROM video_downloads AS kept
                WHERE kept.id = (
                    SELECT candidate.id
                    FROM video_downloads AS candidate
                    WHERE candidate.animeId = kept.animeId
                      AND candidate.episode = kept.episode
                    ORDER BY candidate.updatedAt DESC, candidate.id DESC
                    LIMIT 1
                )
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_video_downloads_duplicate_key
            ON video_downloads(animeId, episode)
            """.trimIndent()
        )
    }
}

internal val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE search_items ADD COLUMN year INTEGER")
    }
}

internal val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE anime_top_items ADD COLUMN year INTEGER")
    }
}

internal val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE collection_anime_items ADD COLUMN year INTEGER")
    }
}

internal val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE home_feed_items ADD COLUMN year INTEGER")
    }
}

internal val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS remote_continue_watching")
    }
}

internal val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE collection_details " +
                    "ADD COLUMN ownerId INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE collection_details " +
                    "ADD COLUMN isPublic INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/**
 * Normalizes databases created by an intermediate version 38 build.
 * Their schema already matches version 39, but Room must validate it and store the current hash.
 */
internal val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) = Unit
}

internal val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE anime_detail_named_items ADD COLUMN itemUrl TEXT")
        // Старый кэш не содержит URL студий. Инвалидируем только детали аниме,
        // чтобы первый показ заново получил кликабельные метаданные из API.
        db.execSQL("DELETE FROM anime_details")
    }
}

internal val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE anime_details ADD COLUMN reviewsCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS document_cache (" +
                    "cacheKey TEXT NOT NULL, payload TEXT NOT NULL, cachedAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(cacheKey))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_document_cache_cachedAt " +
                    "ON document_cache(cachedAt)"
        )
    }
}

/**
 * Normalizes databases created before reviewsCount declared its Room default value.
 * The physical schema is unchanged; completing the migration stores the current identity hash.
 */
internal val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(db: SupportSQLiteDatabase) = Unit
}

internal val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE video_downloads ADD COLUMN exportStatus TEXT NOT NULL DEFAULT 'Idle'")
        db.execSQL("ALTER TABLE video_downloads ADD COLUMN exportProgress REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE video_downloads ADD COLUMN exportDirectoryUri TEXT")
        db.execSQL("ALTER TABLE video_downloads ADD COLUMN exportedFileUri TEXT")
        db.execSQL("ALTER TABLE video_downloads ADD COLUMN exportErrorMessage TEXT")
    }
}

internal val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE anime_details ADD COLUMN malId INTEGER")
    }
}

internal val MIGRATION_44_45 = object : Migration(44, 45) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS alloha_track_preference (" +
                    "animeId INTEGER NOT NULL, dubbing TEXT NOT NULL, player TEXT NOT NULL, " +
                    "audioLabel TEXT, subtitleLanguage TEXT, subtitleLabel TEXT, " +
                    "subtitleOff INTEGER NOT NULL DEFAULT 0, updatedAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(animeId, dubbing, player))"
        )
    }
}

internal val MIGRATION_45_46 = object : Migration(45, 46) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS video_subscription_selection (" +
                    "userId INTEGER NOT NULL, animeId INTEGER NOT NULL, " +
                    "playerKey TEXT NOT NULL, dubbingKey TEXT NOT NULL, " +
                    "videoId INTEGER NOT NULL, updatedAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(userId, animeId, playerKey, dubbingKey))"
        )
    }
}

internal val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE anime_videos ADD COLUMN subscribed INTEGER NOT NULL DEFAULT 0")
        // Состояние подписки отдаёт сам сервер в /anime/{id}/videos — локальная таблица не нужна.
        db.execSQL("DROP TABLE IF EXISTS video_subscription_selection")
        // Флаг subscribed появляется только в авторизованном ответе: помечаем кэш видео устаревшим,
        // чтобы первый показ перечитал его с сервера.
        db.execSQL("UPDATE anime_video_caches SET cachedAt = 0")
    }
}

/**
 * Схема ключей кэша для загрузок. Существующие строки остаются в схеме 0 (legacy): их HLS/DASH
 * сегменты лежат под сырыми URL и по ним неотличимы от чужих, поэтому такие загрузки продолжают
 * читаться и удаляться по-старому, а новые пишутся уже в неймспейс своего cacheKey.
 */
internal val MIGRATION_47_48 = object : Migration(47, 48) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE video_downloads ADD COLUMN cacheKeyScheme INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Год выхода тайтла в библиотеке. Приходит вместе со списками пользователя и из деталей, поэтому
 * старые записи заполняются сами при ближайшей синхронизации — разовый бэкфилл не нужен.
 */
internal val MIGRATION_48_49 = object : Migration(48, 49) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE library ADD COLUMN year INTEGER")
    }
}

/**
 * Дата выхода следующей серии. Приходит вместе со списками пользователя, поэтому старые записи
 * заполняются сами при ближайшей синхронизации — разовый бэкфилл не нужен.
 */
internal val MIGRATION_49_50 = object : Migration(49, 50) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE account_user_list_items ADD COLUMN nextEpisodeAtSeconds INTEGER"
        )
        db.execSQL("ALTER TABLE library ADD COLUMN nextEpisodeAtSeconds INTEGER")
    }
}

/**
 * Общий рейтинг тайтла в библиотеке — нужен для сортировки списков. Приходит вместе со списками
 * пользователя, поэтому старые записи заполняются сами при ближайшей синхронизации.
 */
internal val MIGRATION_50_51 = object : Migration(50, 51) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE library ADD COLUMN rating REAL")
    }
}

/**
 * Сезон выхода тайтла (квартал года). Приходит вместе со списками пользователя, поэтому старые
 * записи заполняются сами при ближайшей синхронизации — разовый бэкфилл не нужен.
 */
internal val MIGRATION_51_52 = object : Migration(51, 52) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE account_user_list_items ADD COLUMN season TEXT")
        db.execSQL("ALTER TABLE library ADD COLUMN season TEXT")
    }
}

/**
 * Локальный список отложенных серий. Серверного аналога нет, список начинается пустым —
 * бэкфилл не нужен.
 */
internal val MIGRATION_52_53 = object : Migration(52, 53) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS watch_later (
                animeId INTEGER NOT NULL,
                episode TEXT NOT NULL,
                animeTitle TEXT NOT NULL,
                posterUrl TEXT NOT NULL,
                screenshotUrl TEXT NOT NULL,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY(animeId, episode)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_watch_later_addedAt ON watch_later (addedAt)")
    }
}

internal val MIGRATION_53_54 = object : Migration(53, 54) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pending_mutations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                payloadJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                attemptCount INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
