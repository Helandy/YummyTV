package su.afk.yummy.tv.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import su.afk.yummy.tv.core.network.YaniApiJson
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = YaniApiJson

    // Общий пул соединений для API и картинок; таймауты настраиваются в Ktor HttpTimeout.
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()
}
