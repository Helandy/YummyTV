package su.afk.yummy.tv.core.tv

import android.content.Context
import android.content.res.Configuration
import androidx.activity.result.ActivityResultCaller
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ITvIntegration] инжектится и в TV-специфичные места ([TvActivity][su.afk.yummy.tv.android.TvActivity]),
 * и в общие ([SettingsViewModel][su.afk.yummy.tv.feature.settings.SettingsViewModel], который работает
 * и на мобильном). Этот класс — device-guard: делегирует в [real] только на телевизоре, иначе отдаёт
 * no-op/пустой флоу, чтобы вызывающему коду (включая мобильный) не нужно было самому проверять
 * [isTelevision] перед каждым вызовом и не задевать TvProvider/TvContract API вне TV.
 *
 * При добавлении нового метода в [ITvIntegration] не забыть продублировать guard здесь.
 */
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

    override val isSupported: Boolean
        get() = isTelevision

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

    override fun bindBrowsableChannelRequests(
        activityResultCaller: ActivityResultCaller,
        scope: CoroutineScope,
    ) {
        if (isTelevision) real.bindBrowsableChannelRequests(activityResultCaller, scope)
    }
}
