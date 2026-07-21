package su.afk.yummy.tv.feature.faq.mobile.navigator

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.faq.mobile.FaqMobileScreen
import su.afk.yummy.tv.feature.faq.navigator.FaqDestination

class FaqNavRegistrar : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<FaqDestination> {
                FaqMobileScreen(onBack = nav::back)
            }
        }
}
