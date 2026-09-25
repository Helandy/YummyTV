package su.afk.yummy.tv.feature.faq.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.faq.FaqViewModel
import su.afk.yummy.tv.feature.faq.IMobileFaqEntry
import su.afk.yummy.tv.feature.faq.mobile.FaqMobileScreen
import su.afk.yummy.tv.feature.faq.navigator.FaqDestination
import javax.inject.Inject

class FaqNavRegistrar @Inject constructor() : IMobileFaqEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<FaqDestination> {
                val vm = hiltViewModel<FaqViewModel>()
                ScreenNavigator(vm) { state, effect, onEvent ->
                    FaqMobileScreen(state, effect, onEvent)
                }
            }
        }
}
