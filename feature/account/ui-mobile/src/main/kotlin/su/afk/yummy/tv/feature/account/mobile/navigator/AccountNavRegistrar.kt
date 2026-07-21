package su.afk.yummy.tv.feature.account.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.account.account.AccountViewModel
import su.afk.yummy.tv.feature.account.mobile.account.AccountMobileScreen
import su.afk.yummy.tv.feature.account.mobile.passwordreset.PasswordResetMobileScreen
import su.afk.yummy.tv.feature.account.mobile.profileedit.ProfileEditMobileScreen
import su.afk.yummy.tv.feature.account.mobile.userprofile.UserProfileMobileScreen
import su.afk.yummy.tv.feature.account.mobile.userprofile.UserProfileResolverMobileScreen
import su.afk.yummy.tv.feature.account.mobile.usersearch.UserSearchMobileScreen
import su.afk.yummy.tv.feature.account.navigator.AccountDestination
import su.afk.yummy.tv.feature.account.navigator.PasswordResetDestination
import su.afk.yummy.tv.feature.account.navigator.ProfileEditDestination
import su.afk.yummy.tv.feature.account.navigator.UserProfileByNicknameDestination
import su.afk.yummy.tv.feature.account.navigator.UserProfileDestination
import su.afk.yummy.tv.feature.account.navigator.UserSearchDestination
import su.afk.yummy.tv.feature.account.passwordreset.PasswordResetViewModel
import su.afk.yummy.tv.feature.account.profileedit.ProfileEditViewModel
import su.afk.yummy.tv.feature.account.userprofile.UserProfileResolverViewModel
import su.afk.yummy.tv.feature.account.userprofile.UserProfileViewModel
import su.afk.yummy.tv.feature.account.usersearch.UserSearchViewModel
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
            entry<UserProfileByNicknameDestination> { dest ->
                val viewModel = hiltViewModel<
                        UserProfileResolverViewModel,
                        UserProfileResolverViewModel.Factory,
                        >(
                    key = "mobile-user-profile-nickname-${dest.nickname}",
                    creationCallback = { factory -> factory.create(dest.nickname) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    UserProfileResolverMobileScreen(state, effect, onEvent)
                }
            }
            entry<UserSearchDestination> {
                val viewModel = hiltViewModel<UserSearchViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    UserSearchMobileScreen(state, effect, onEvent)
                }
            }
            entry<ProfileEditDestination> {
                val viewModel = hiltViewModel<ProfileEditViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    ProfileEditMobileScreen(state, effect, onEvent)
                }
            }
            entry<PasswordResetDestination> {
                val viewModel = hiltViewModel<PasswordResetViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    PasswordResetMobileScreen(state, effect, onEvent)
                }
            }
        }
}
