package su.afk.yummy.tv.feature.messages.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.messages.IMessagesNavigator
import su.afk.yummy.tv.feature.messages.navigator.MessagesNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MessagesNavigatorModule {

    @Binds
    @Singleton
    fun bindMessagesNavigator(impl: MessagesNavigator): IMessagesNavigator
}
