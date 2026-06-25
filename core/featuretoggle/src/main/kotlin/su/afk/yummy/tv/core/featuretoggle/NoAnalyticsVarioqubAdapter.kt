package su.afk.yummy.tv.core.featuretoggle

import com.yandex.varioqub.analyticadapter.AdapterIdentifiersCallback
import com.yandex.varioqub.analyticadapter.VarioqubConfigAdapter
import com.yandex.varioqub.analyticadapter.data.ConfigData
import java.util.UUID

internal class NoAnalyticsVarioqubAdapter : VarioqubConfigAdapter {

    override val adapterName: String = ADAPTER_NAME
    private val deviceId: String = UUID.randomUUID().toString()

    override fun requestDeviceId(callback: AdapterIdentifiersCallback) {
        callback.onSuccess(deviceId)
    }

    override fun requestUserId(callback: AdapterIdentifiersCallback) {
        callback.onSuccess(deviceId)
    }

    override fun setExperiments(experiments: String) = Unit

    override fun setTriggeredTestIds(triggeredTestIds: Set<Long>) = Unit

    override fun reportConfigChanged(configData: ConfigData) = Unit

    private companion object {
        const val ADAPTER_NAME = "no_analytics"
    }
}
