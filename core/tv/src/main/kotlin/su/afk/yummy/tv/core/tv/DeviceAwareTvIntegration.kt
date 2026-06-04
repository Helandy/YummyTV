package su.afk.yummy.tv.core.tv

import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DeviceAwareTvIntegration @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val real: TvIntegration,
) : ITvIntegration {

    private val emptyBrowsableRequests = MutableSharedFlow<Long>()
    private val notBrowsable = MutableStateFlow(false)

    private val isTelevision: Boolean
        get() = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION

    override val browsableChannelRequest: SharedFlow<Long>
        get() = if (isTelevision) real.browsableChannelRequest else emptyBrowsableRequests

    override val previewChannelBrowsable: StateFlow<Boolean>
        get() = if (isTelevision) real.previewChannelBrowsable else notBrowsable

    override fun start() {
        if (isTelevision) real.start()
    }

    override fun requestPreviewChannelBrowsable() {
        if (isTelevision) real.requestPreviewChannelBrowsable()
    }

    override fun refreshPreviewChannelStatus() {
        if (isTelevision) real.refreshPreviewChannelStatus()
    }
}
