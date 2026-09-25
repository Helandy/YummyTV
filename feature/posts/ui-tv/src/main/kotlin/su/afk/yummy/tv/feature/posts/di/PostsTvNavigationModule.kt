package su.afk.yummy.tv.feature.posts.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.posts.ITvPostsEntry
import su.afk.yummy.tv.feature.posts.tv.navigator.PostsNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface PostsTvNavigationModule {

    @Binds
    fun bindPostsNavRegistrar(impl: PostsNavRegistrar): ITvPostsEntry
}
