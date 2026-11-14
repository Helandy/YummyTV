package su.afk.yummy.tv.source.anime4k.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider
import su.afk.yummy.tv.source.anime4k.data.BuildConfig
import su.afk.yummy.tv.source.anime4k.data.flag.Anime4kFeatureGate
import su.afk.yummy.tv.source.anime4k.data.repository.Anime4kRepositoryImpl
import su.afk.yummy.tv.source.anime4k.data.source.AndroidAnime4kMessages
import su.afk.yummy.tv.source.anime4k.data.source.Anime4kExtraStreamRepository
import su.afk.yummy.tv.source.anime4k.data.source.Anime4kMessages
import su.afk.yummy.tv.source.anime4k.data.source.Anime4kExtraVideoRepository
import su.afk.yummy.tv.source.anime4k.domain.repository.Anime4kRepository
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface Anime4kModule {

    @Binds
    @Singleton
    fun bindAnime4kRepository(impl: Anime4kRepositoryImpl): Anime4kRepository

    @Binds
    fun bindMessages(impl: AndroidAnime4kMessages): Anime4kMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: Anime4kExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: Anime4kExtraStreamRepository): ExtraStreamRepository

    companion object {
        @Provides
        @Singleton
        fun provideFeatureGate(featureToggleProvider: FeatureToggleProvider): Anime4kFeatureGate =
            Anime4kFeatureGate(featureToggleProvider, forceEnabled = BuildConfig.DEBUG)
    }
}
