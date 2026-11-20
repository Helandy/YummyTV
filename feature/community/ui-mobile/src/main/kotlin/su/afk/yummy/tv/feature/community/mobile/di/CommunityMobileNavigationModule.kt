package su.afk.yummy.tv.feature.community.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.community.IMobileCommunityEntry
import su.afk.yummy.tv.feature.community.mobile.navigator.CommunityNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface CommunityMobileNavigationModule {

    @Binds
    fun bindCommunityNavRegistrar(impl: CommunityNavRegistrar): IMobileCommunityEntry
}
