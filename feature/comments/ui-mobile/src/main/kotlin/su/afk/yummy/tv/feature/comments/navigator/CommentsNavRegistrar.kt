package su.afk.yummy.tv.feature.comments.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.comments.CommentsMobileScreen
import su.afk.yummy.tv.feature.comments.CommentsViewModel
import su.afk.yummy.tv.feature.comments.navigator.AnimeCommentsDestination
import javax.inject.Inject

class CommentsNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<AnimeCommentsDestination> { dest ->
                val viewModel = hiltViewModel<CommentsViewModel, CommentsViewModel.Factory>(
                    key = "mobile-comments-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    CommentsMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
