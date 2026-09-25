package su.afk.yummy.tv.feature.faq.mobile.navigator

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.faq.IMobileFaqEntry
import su.afk.yummy.tv.feature.faq.mobile.FaqMobileScreen
import su.afk.yummy.tv.feature.faq.mobile.model.FaqState
import su.afk.yummy.tv.feature.faq.navigator.FaqDestination
import javax.inject.Inject

class FaqNavRegistrar @Inject constructor() : IMobileFaqEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
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
