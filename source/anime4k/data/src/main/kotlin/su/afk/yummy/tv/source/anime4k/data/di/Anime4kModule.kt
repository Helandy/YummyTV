package su.afk.yummy.tv.source.anime4k.data.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider
import su.afk.yummy.tv.source.anime4k.data.BuildConfig
import su.afk.yummy.tv.source.anime4k.data.flag.Anime4kFeatureGate
import su.afk.yummy.tv.source.anime4k.data.ads.YandexRewardedAdRepository
import su.afk.yummy.tv.source.anime4k.data.repository.Anime4kRepositoryImpl
import su.afk.yummy.tv.source.anime4k.data.signing.AppCertificate
import su.afk.yummy.tv.source.anime4k.data.signing.RequestSigner
import su.afk.yummy.tv.source.anime4k.data.signing.SignatureHeaders
import su.afk.yummy.tv.source.anime4k.data.source.AndroidAnime4kMessages
import su.afk.yummy.tv.source.anime4k.data.source.Anime4kExtraStreamRepository
import su.afk.yummy.tv.source.anime4k.data.source.Anime4kMessages
import su.afk.yummy.tv.source.anime4k.data.source.Anime4kExtraVideoRepository
import su.afk.yummy.tv.source.anime4k.domain.repository.Anime4kRepository
import su.afk.yummy.tv.source.anime4k.domain.repository.RewardedAdRepository
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import java.security.SecureRandom
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface Anime4kModule {

    @Binds
    @Singleton
    fun bindAnime4kRepository(impl: Anime4kRepositoryImpl): Anime4kRepository

    @Binds
    @Singleton
    fun bindRewardedAdRepository(impl: YandexRewardedAdRepository): RewardedAdRepository

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

        @Provides
        @Singleton
        fun provideRequestSigner(
            @ApplicationContext context: Context,
            siteApiConfig: SiteApiConfig,
        ): RequestSigner =
            SignatureHeaders(
                secret = siteApiConfig.effectiveToken(BuildConfig.API_SIGNING_SECRET),
                certSha256Hex = AppCertificate.sha256Hex(context),
                appVersion = AppCertificate.versionName(context),
                nowSeconds = { System.currentTimeMillis() / 1000 },
                nonce = { ByteArray(16).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) } },
            )

        /** Тот же подписчик подписывает и остальные запросы к API сайта (например, названия серий). */
        @Provides
        @IntoSet
        fun provideSiteRequestSigner(signer: RequestSigner): SiteRequestSigner = signer
    }
}
