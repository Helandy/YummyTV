package su.afk.yummy.tv.feature.bloggers.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.bloggers.navigator.BloggerVideosNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface BloggersNavigatorModule {

    @Binds
    @Singleton
    fun bindBloggerVideosNavigator(impl: BloggerVideosNavigator): IBloggerVideosNavigator
}
