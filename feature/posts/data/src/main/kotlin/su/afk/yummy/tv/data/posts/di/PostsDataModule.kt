package su.afk.yummy.tv.data.posts.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.data.posts.repository.YaniPostsRepository
import su.afk.yummy.tv.domain.posts.repository.PostsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class PostsDataModule {
    @Binds
    abstract fun bindPostsRepository(impl: YaniPostsRepository): PostsRepository
}
