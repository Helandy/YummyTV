package su.afk.yummy.tv.core.tv.api

import androidx.activity.result.ActivityResultCaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ITvIntegration {
    /** `true` только на телевизоре: на остальных устройствах preview-каналов нет. */
    val isSupported: Boolean
    val browsableChannelRequest: SharedFlow<Long>
    val previewChannelBrowsable: StateFlow<Boolean>
    fun start()
    fun requestPreviewChannelBrowsable()
    fun refreshPreviewChannelStatus()

    /**
     * Регистрирует лаунчер системного запроса на добавление preview-канала в браузер каналов
     * и подписывается на [browsableChannelRequest]. Должен вызываться синхронно из onCreate,
     * до перехода activity в STARTED (требование ActivityResultCaller).
     */
    fun bindBrowsableChannelRequests(
        activityResultCaller: ActivityResultCaller,
        scope: CoroutineScope
    )
}
