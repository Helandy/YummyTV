package su.afk.yummy.tv.feature.bloggers.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.bloggers.ITvBloggerVideosEntry
import su.afk.yummy.tv.feature.bloggers.tv.navigator.BloggerVideosNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface BloggerVideosTvNavigationModule {

    @Binds
    fun bindBloggerVideosNavRegistrar(impl: BloggerVideosNavRegistrar): ITvBloggerVideosEntry
}
