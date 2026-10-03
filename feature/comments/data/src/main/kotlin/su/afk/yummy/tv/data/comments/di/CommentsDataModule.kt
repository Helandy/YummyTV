package su.afk.yummy.tv.data.comments.di

import su.afk.yummy.tv.core.utils.coroutines.AppDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.comments.CommentsStorage
import su.afk.yummy.tv.data.comments.network.YaniCommentsApi
import su.afk.yummy.tv.data.comments.repository.YaniCommentsRepository
import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommentsDataModule {

    @Provides
    @Singleton
    fun provideYaniCommentsApi(clientProvider: YaniHttpClientProvider): YaniCommentsApi =
        YaniCommentsApi(clientProvider)

    @Provides
    @Singleton
    fun provideCommentsRepository(
        api: YaniCommentsApi,
        commentsStorage: CommentsStorage,
        settingsStore: YaniAccountSettingsStore,
        dispatchers: AppDispatchers,
    ): CommentsRepository =
        YaniCommentsRepository(api, commentsStorage, settingsStore, dispatchers)
}
