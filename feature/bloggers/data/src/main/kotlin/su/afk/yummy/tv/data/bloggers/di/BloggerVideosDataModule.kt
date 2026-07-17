package su.afk.yummy.tv.data.bloggers.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.data.bloggers.repository.YaniBloggerVideosRepository
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BloggerVideosDataModule {
    @Binds
    @Singleton
    abstract fun bindRepository(repository: YaniBloggerVideosRepository): BloggerVideosRepository
}
