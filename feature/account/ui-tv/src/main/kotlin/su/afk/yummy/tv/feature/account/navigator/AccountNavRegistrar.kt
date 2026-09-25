package su.afk.yummy.tv.feature.account.tv.navigator

import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.account.ITvAccountEntry
import su.afk.yummy.tv.feature.account.account.AccountTvScreen
import su.afk.yummy.tv.feature.account.account.AccountViewModel
import su.afk.yummy.tv.feature.account.mysubscriptions.MySubscriptionsTvScreen
import su.afk.yummy.tv.feature.account.mysubscriptions.MySubscriptionsViewModel
import su.afk.yummy.tv.feature.account.navigator.AccountDestination
import su.afk.yummy.tv.feature.account.navigator.MySubscriptionsDestination
import su.afk.yummy.tv.feature.account.utils.LocalAccountTvActiveDestination
import javax.inject.Inject

class AccountNavRegistrar @Inject constructor() : ITvAccountEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<AccountDestination> {
                val viewModel = hiltViewModel<AccountViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    val isActiveDestination = nav.appBackStack.isEmpty() &&
                            nav.backStack.lastOrNull() == AccountDestination
                    CompositionLocalProvider(LocalAccountTvActiveDestination provides isActiveDestination) {
                        AccountTvScreen(state = state, effect = effect, onEvent = onEvent)
                    }
                }
            }
            entry<MySubscriptionsDestination> {
                val viewModel = hiltViewModel<MySubscriptionsViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    MySubscriptionsTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
