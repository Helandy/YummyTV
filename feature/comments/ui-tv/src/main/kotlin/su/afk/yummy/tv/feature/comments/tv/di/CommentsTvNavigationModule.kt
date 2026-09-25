package su.afk.yummy.tv.feature.comments.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.comments.ITvCommentsEntry
import su.afk.yummy.tv.feature.comments.tv.navigator.CommentsNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface CommentsTvNavigationModule {

    @Binds
    fun bindCommentsNavRegistrar(impl: CommentsNavRegistrar): ITvCommentsEntry
}
