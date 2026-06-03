package su.afk.yummy.tv.core.tv.api

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ITvIntegration {
    val browsableChannelRequest: SharedFlow<Long>
    val previewChannelBrowsable: StateFlow<Boolean>
    fun start()
    fun requestPreviewChannelBrowsable()
    fun refreshPreviewChannelStatus()
}
