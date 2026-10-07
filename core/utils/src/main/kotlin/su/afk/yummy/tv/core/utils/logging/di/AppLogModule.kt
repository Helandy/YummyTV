package su.afk.yummy.tv.core.utils.logging.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.analytics.api.DiagnosticLogSink
import su.afk.yummy.tv.core.utils.logging.AppLogDiagnosticSink

@Module
@InstallIn(SingletonComponent::class)
abstract class AppLogModule {

    @Binds
    abstract fun bindDiagnosticLogSink(impl: AppLogDiagnosticSink): DiagnosticLogSink
}
