package su.afk.yummy.tv.feature.account.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.account.account.AccountMobileScreen
import su.afk.yummy.tv.feature.account.account.AccountViewModel
import su.afk.yummy.tv.feature.account.navigator.AccountDestination
import su.afk.yummy.tv.feature.account.navigator.UserProfileDestination
import su.afk.yummy.tv.feature.account.userprofile.UserProfileMobileScreen
import su.afk.yummy.tv.feature.account.userprofile.UserProfileViewModel
import javax.inject.Inject

class AccountNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<AccountDestination> {
                val viewModel = hiltViewModel<AccountViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    AccountMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<UserProfileDestination> { dest ->
                val viewModel = hiltViewModel<UserProfileViewModel, UserProfileViewModel.Factory>(
                    key = "mobile-user-profile-${dest.userId}",
                    creationCallback = { factory -> factory.create(dest.userId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    UserProfileMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
