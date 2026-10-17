package su.afk.yummy.tv.core.storage.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.storage.db.AppDatabase
import su.afk.yummy.tv.core.storage.db.migrations.ALL_MIGRATIONS
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "yummy_cache.db")
            .addMigrations(*ALL_MIGRATIONS)
            .fallbackToDestructiveMigrationFrom(
                dropAllTables = true,
                1,
                2,
                3,
                4,
                5,
                6,
            )
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
}
