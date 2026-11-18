package su.afk.yummy.tv.feature.player.extractor.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import su.afk.yummy.tv.feature.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.feature.player.extractor.network.KtorPlayerHttpClient
import su.afk.yummy.tv.feature.player.extractor.network.PlayerHttpClient
import javax.inject.Singleton

/**
 * Общая часть экстракторов балансеров. Набор [PlayerStreamExtractor] допустимо пустой: модули
 * `source/<балансер>/data` необязательны и дописывают в него свой экстрактор через `@IntoSet`.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface PlayerExtractorModule {

    @Binds
    @Singleton
    fun bindPlayerHttpClient(client: KtorPlayerHttpClient): PlayerHttpClient

    @Multibinds
    fun playerStreamExtractors(): Set<@JvmSuppressWildcards PlayerStreamExtractor>
}
