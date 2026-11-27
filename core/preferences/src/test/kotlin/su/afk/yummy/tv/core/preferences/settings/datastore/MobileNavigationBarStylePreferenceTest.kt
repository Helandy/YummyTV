package su.afk.yummy.tv.core.preferences.settings.datastore

import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.MobileNavigationBarStyle
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.mobileNavigationBarStyleKey
import su.afk.yummy.tv.core.testing.BaseUnitTest

class MobileNavigationBarStylePreferenceTest : BaseUnitTest() {
    @Test
    fun `missing and unknown values use the standard bar`() {
        assertEquals(
            MobileNavigationBarStyle.STANDARD,
            preferencesOf().enum(mobileNavigationBarStyleKey, MobileNavigationBarStyle.STANDARD),
        )
        assertEquals(
            MobileNavigationBarStyle.STANDARD,
            preferencesOf(mobileNavigationBarStyleKey to "UNKNOWN").enum(mobileNavigationBarStyleKey, MobileNavigationBarStyle.STANDARD),
        )
    }

    @Test
    fun `stored floating value is read`() {
        val prefs = preferencesOf(mobileNavigationBarStyleKey to MobileNavigationBarStyle.FLOATING.name)
        assertEquals(MobileNavigationBarStyle.FLOATING, prefs.enum(mobileNavigationBarStyleKey, MobileNavigationBarStyle.STANDARD))
    }
}
