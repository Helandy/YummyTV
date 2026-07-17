package su.afk.yummy.tv.data.messages.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.data.messages.repository.YaniMessagesRepository
import su.afk.yummy.tv.domain.messages.repository.MessagesRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class MessagesDataModule {
    @Binds
    abstract fun bindMessagesRepository(impl: YaniMessagesRepository): MessagesRepository
}
