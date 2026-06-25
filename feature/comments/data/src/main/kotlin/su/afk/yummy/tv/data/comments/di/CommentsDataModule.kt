package su.afk.yummy.tv.data.comments.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.core.storage.comments.CommentsStorageStore
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
        commentsStorage: CommentsStorageStore,
    ): CommentsRepository =
        YaniCommentsRepository(api, commentsStorage)
}
