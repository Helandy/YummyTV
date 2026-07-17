package su.afk.yummy.tv.feature.posts.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.posts.details.PostDetailsTvScreen
import su.afk.yummy.tv.feature.posts.details.PostDetailsViewModel
import su.afk.yummy.tv.feature.posts.list.PostsListViewModel
import su.afk.yummy.tv.feature.posts.list.PostsTvScreen
import su.afk.yummy.tv.feature.posts.navigator.PostDetailsDestination
import su.afk.yummy.tv.feature.posts.navigator.PostsDestination
import javax.inject.Inject

class PostsNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<PostsDestination> {
                val vm = hiltViewModel<PostsListViewModel>()
                ScreenNavigator(vm) { state, effect, events ->
                    PostsTvScreen(
                        state,
                        effect,
                        events
                    )
                }
            }
            entry<PostDetailsDestination> { destination ->
                val vm =
                    hiltViewModel<PostDetailsViewModel, PostDetailsViewModel.Factory>(key = "tv-post-${destination.postId}") {
                        it.create(destination.postId)
                    }
                ScreenNavigator(vm) { state, effect, events ->
                    PostDetailsTvScreen(
                        state,
                        effect,
                        events
                    )
                }
            }
        }
}
