package su.afk.yummy.tv.core.error.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.error.AndroidStringProvider
import su.afk.yummy.tv.core.error.ErrorHandlerUseCaseImpl
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider

@Module
@InstallIn(SingletonComponent::class)
interface ErrorModule {

    @Binds
    fun bindStringProvider(impl: AndroidStringProvider): StringProvider

    @Binds
    fun bindErrorHandlerUseCase(impl: ErrorHandlerUseCaseImpl): IErrorHandlerUseCase
}