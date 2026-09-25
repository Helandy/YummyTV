package su.afk.yummy.tv.feature.comments.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.comments.IMobileCommentsEntry
import su.afk.yummy.tv.feature.comments.mobile.navigator.CommentsNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface CommentsMobileNavigationModule {

    @Binds
    fun bindCommentsNavRegistrar(impl: CommentsNavRegistrar): IMobileCommentsEntry
}
