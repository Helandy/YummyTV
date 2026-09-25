package su.afk.yummy.tv.feature.comments.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.comments.navigator.CommentsNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CommentsNavigatorModule {

    @Binds
    @Singleton
    fun bindCommentsNavigator(impl: CommentsNavigator): ICommentsNavigator
}
