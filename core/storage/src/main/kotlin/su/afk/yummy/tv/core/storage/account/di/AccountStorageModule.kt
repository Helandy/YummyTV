package su.afk.yummy.tv.core.storage.account.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.db.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountStorageModule {

    @Provides
    @Singleton
    internal fun provideAccountStorageStore(db: AppDatabase): AccountStorageStore =
        AccountStorageStore(
            db = db,
            profileDao = db.accountProfileDao(),
            userListsDao = db.accountUserListsDao(),
            animeRatingsDao = db.accountAnimeRatingsDao(),
            collectionsDao = db.accountCollectionsDao(),
            videoSubscriptionsDao = db.accountVideoSubscriptionsDao(),
            notificationsDao = db.accountNotificationsDao(),
            userProfileDao = db.accountUserProfileDao(),
        )

    @Provides
    @Singleton
    internal fun provideAccountStorage(store: AccountStorageStore): AccountStorage = store
}
