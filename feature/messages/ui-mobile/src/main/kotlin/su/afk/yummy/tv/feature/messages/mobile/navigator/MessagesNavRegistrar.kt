package su.afk.yummy.tv.feature.messages.mobile.navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.mobile.MobileDetailPlaceholder
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.scene.listPaneAnchor
import su.afk.yummy.tv.feature.messages.IMobileMessagesEntry
import su.afk.yummy.tv.feature.messages.chat.ChatViewModel
import su.afk.yummy.tv.feature.messages.dialogs.DialogsViewModel
import su.afk.yummy.tv.feature.messages.mobile.R
import su.afk.yummy.tv.feature.messages.mobile.chat.ChatMobileScreen
import su.afk.yummy.tv.feature.messages.mobile.dialogs.DialogsMobileScreen
import su.afk.yummy.tv.feature.messages.navigator.ChatDestination
import su.afk.yummy.tv.feature.messages.navigator.DialogsDestination
import javax.inject.Inject

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class MessagesNavRegistrar @Inject constructor() : IMobileMessagesEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<DialogsDestination>(metadata = listPaneMetadata()) {
                val viewModel = hiltViewModel<DialogsViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    DialogsMobileScreen(state, effect, onEvent)
                }
            }
            entry<ChatDestination>(metadata = ListDetailSceneStrategy.detailPane(LIST_DETAIL_SCENE_KEY)) { destination ->
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

    private fun listPaneMetadata() = listPaneAnchor() + ListDetailSceneStrategy.listPane(
        sceneKey = LIST_DETAIL_SCENE_KEY,
        detailPlaceholder = {
            MobileDetailPlaceholder(
                icon = Icons.Outlined.Forum,
                text = stringResource(R.string.messages_mobile_detail_placeholder),
            )
        },
    )
}

private const val LIST_DETAIL_SCENE_KEY = "messages"
