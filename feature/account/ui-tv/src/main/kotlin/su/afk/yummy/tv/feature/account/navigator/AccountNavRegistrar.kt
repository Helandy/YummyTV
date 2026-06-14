package su.afk.yummy.tv.feature.account.tv.navigator

import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.account.AccountTvScreen
import su.afk.yummy.tv.feature.account.AccountViewModel
import su.afk.yummy.tv.feature.account.navigator.AccountDestination
import su.afk.yummy.tv.feature.account.utils.LocalAccountTvActiveDestination
import javax.inject.Inject

class AccountNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
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
        }
}
