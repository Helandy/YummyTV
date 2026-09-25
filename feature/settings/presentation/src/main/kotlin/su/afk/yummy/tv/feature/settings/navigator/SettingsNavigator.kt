package su.afk.yummy.tv.feature.settings.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import javax.inject.Inject

class SettingsNavigator @Inject constructor() : ISettingsNavigator {
    override fun getSettingsDest(): NavKey = SettingsDestination
}
