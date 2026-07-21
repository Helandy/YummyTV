package su.afk.yummy.tv.feature.messages.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.messages.chat.ChatViewModel
import su.afk.yummy.tv.feature.messages.dialogs.DialogsViewModel
import su.afk.yummy.tv.feature.messages.mobile.chat.ChatMobileScreen
import su.afk.yummy.tv.feature.messages.mobile.dialogs.DialogsMobileScreen
import su.afk.yummy.tv.feature.messages.navigator.ChatDestination
import su.afk.yummy.tv.feature.messages.navigator.DialogsDestination
import javax.inject.Inject

class MessagesNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<DialogsDestination> {
                val viewModel = hiltViewModel<DialogsViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    DialogsMobileScreen(state, effect, onEvent)
                }
            }
            entry<ChatDestination> { destination ->
                val viewModel = hiltViewModel<ChatViewModel, ChatViewModel.Factory>(
                    key = "mobile-chat-${destination.userId}",
                    creationCallback = {
                        it.create(
                            destination.userId,
                            destination.nickname,
                            destination.avatarUrl,
                        )
                    },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    ChatMobileScreen(state, effect, onEvent)
                }
            }
        }
}
