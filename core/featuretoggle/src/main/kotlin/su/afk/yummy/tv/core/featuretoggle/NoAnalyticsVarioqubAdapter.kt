package su.afk.yummy.tv.core.featuretoggle

import android.content.Context
import com.yandex.varioqub.analyticadapter.AdapterIdentifiersCallback
import com.yandex.varioqub.analyticadapter.VarioqubConfigAdapter
import com.yandex.varioqub.analyticadapter.data.ConfigData
import java.util.UUID

internal class NoAnalyticsVarioqubAdapter(
    context: Context,
) : VarioqubConfigAdapter {

    override val adapterName: String = ADAPTER_NAME
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun requestDeviceId(callback: AdapterIdentifiersCallback) {
        callback.onSuccess(deviceId())
    }

    override fun requestUserId(callback: AdapterIdentifiersCallback) {
        callback.onSuccess(deviceId())
    }

    override fun setExperiments(experiments: String) = Unit

    override fun setTriggeredTestIds(triggeredTestIds: Set<Long>) = Unit

    override fun reportConfigChanged(configData: ConfigData) = Unit

    private fun deviceId(): String {
        preferences.getString(KEY_DEVICE_ID, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    private companion object {
        const val ADAPTER_NAME = "no_analytics"
        const val PREFS_NAME = "feature_toggle_no_analytics_adapter"
        const val KEY_DEVICE_ID = "device_id"
    }
}
