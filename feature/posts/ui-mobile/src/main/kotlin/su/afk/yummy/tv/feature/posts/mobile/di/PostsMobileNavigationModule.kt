package su.afk.yummy.tv.feature.posts.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.posts.IMobilePostsEntry
import su.afk.yummy.tv.feature.posts.mobile.navigator.PostsNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface PostsMobileNavigationModule {

    @Binds
    fun bindPostsNavRegistrar(impl: PostsNavRegistrar): IMobilePostsEntry
}
