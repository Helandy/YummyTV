package su.afk.yummy.tv.android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MobileTvIntegrationModule {

    @Binds
    @Singleton
    fun bindTvIntegration(impl: NoOpTvIntegration): ITvIntegration
}

@Singleton
internal class NoOpTvIntegration @Inject constructor() : ITvIntegration {
    override val browsableChannelRequest: SharedFlow<Long> = MutableSharedFlow()
    override val previewChannelBrowsable: StateFlow<Boolean> = MutableStateFlow(false)

    override fun start() = Unit
    override fun requestPreviewChannelBrowsable() = Unit
    override fun refreshPreviewChannelStatus() = Unit
}
