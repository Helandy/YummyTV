package su.afk.yummy.tv.feature.faq.mobile.navigator

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.faq.mobile.FaqMobileScreen
import su.afk.yummy.tv.feature.faq.mobile.model.FaqState
import su.afk.yummy.tv.feature.faq.navigator.FaqDestination

class FaqNavRegistrar : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<FaqDestination> {
                FaqMobileScreen(
                    state = FaqState.State,
                    effect = emptyFlow(),
                    onEvent = { event ->
                        when (event) {
                            FaqState.Event.BackSelected -> nav.back()
                        }
                    },
                )
            }
        }
}
