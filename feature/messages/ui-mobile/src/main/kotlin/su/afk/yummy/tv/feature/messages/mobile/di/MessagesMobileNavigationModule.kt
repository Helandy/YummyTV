package su.afk.yummy.tv.feature.messages.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.messages.IMobileMessagesEntry
import su.afk.yummy.tv.feature.messages.mobile.navigator.MessagesNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface MessagesMobileNavigationModule {

    @Binds
    fun bindMessagesNavRegistrar(impl: MessagesNavRegistrar): IMobileMessagesEntry
}
