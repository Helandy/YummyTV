package su.afk.yummy.tv.feature.comments.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.feature.comments.CommentsViewModel
import su.afk.yummy.tv.feature.comments.ITvCommentsEntry
import su.afk.yummy.tv.feature.comments.navigator.CommentsDestination
import su.afk.yummy.tv.feature.comments.tv.CommentsTvScreen
import javax.inject.Inject

class CommentsNavRegistrar @Inject constructor() : ITvCommentsEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<CommentsDestination> { destination ->
                val targetType = CommentTargetType.valueOf(destination.targetType)
                val viewModel = hiltViewModel<CommentsViewModel, CommentsViewModel.Factory>(
                    key = "comments-tv-${targetType.apiValue}-${destination.targetId}",
                    creationCallback = { factory ->
                        factory.create(targetType, destination.targetId)
                    },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    CommentsTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
