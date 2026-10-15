package su.afk.yummy.tv.data.player.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository

/** Набор допустимо пустой: модули-источники могут отсутствовать. */
@Module
@InstallIn(SingletonComponent::class)
internal interface ExtraStreamRepositoriesModule {

    @Multibinds
    fun extraStreamRepositories(): Set<@JvmSuppressWildcards ExtraStreamRepository>
}
