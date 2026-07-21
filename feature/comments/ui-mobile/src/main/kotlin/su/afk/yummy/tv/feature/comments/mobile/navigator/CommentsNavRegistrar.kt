package su.afk.yummy.tv.feature.comments.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.feature.comments.CommentsViewModel
import su.afk.yummy.tv.feature.comments.mobile.CommentsMobileScreen
import su.afk.yummy.tv.feature.comments.navigator.CommentsDestination
import javax.inject.Inject

class CommentsNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<CommentsDestination> { dest ->
                val targetType = CommentTargetType.valueOf(dest.targetType)
                val viewModel = hiltViewModel<CommentsViewModel, CommentsViewModel.Factory>(
                    key = "comments-${targetType.apiValue}-${dest.targetId}",
                    creationCallback = { factory -> factory.create(targetType, dest.targetId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    CommentsMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
