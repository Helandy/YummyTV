package su.afk.yummy.tv.core.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase
import su.afk.yummy.tv.core.storage.cache.CacheDao
import su.afk.yummy.tv.core.storage.cache.CacheEntry
import su.afk.yummy.tv.core.storage.library.LibraryDao
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressDao
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry

@Database(entities = [CacheEntry::class, LibraryEntry::class, WatchProgressEntry::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
    abstract fun libraryDao(): LibraryDao
    abstract fun watchProgressDao(): WatchProgressDao
}
