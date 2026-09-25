package su.afk.yummy.tv.feature.bloggers.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.bloggers.IMobileBloggerVideosEntry
import su.afk.yummy.tv.feature.bloggers.mobile.navigator.BloggerVideosNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface BloggerVideosMobileNavigationModule {

    @Binds
    fun bindBloggerVideosNavRegistrar(impl: BloggerVideosNavRegistrar): IMobileBloggerVideosEntry
}
