package su.afk.yummy.tv.feature.posts.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.posts.IPostsNavigator
import su.afk.yummy.tv.feature.posts.navigator.PostsNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PostsNavigatorModule {

    @Binds
    @Singleton
    fun bindPostsNavigator(impl: PostsNavigator): IPostsNavigator
}
