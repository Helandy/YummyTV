package su.afk.yummy.tv.data.details.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository

/** Объявляет набор допустимо пустым: модули-источники могут отсутствовать (форк без них собирается как раньше). */
@Module
@InstallIn(SingletonComponent::class)
internal interface ExtraVideoRepositoriesModule {

    @Multibinds
    fun extraVideoRepositories(): Set<@JvmSuppressWildcards ExtraVideoRepository>
}
