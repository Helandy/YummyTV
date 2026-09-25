package su.afk.yummy.tv.feature.posts.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.posts.IMobilePostsEntry
import su.afk.yummy.tv.feature.posts.details.PostDetailsViewModel
import su.afk.yummy.tv.feature.posts.list.PostsListViewModel
import su.afk.yummy.tv.feature.posts.mobile.details.PostDetailsMobileScreen
import su.afk.yummy.tv.feature.posts.mobile.list.PostsMobileScreen
import su.afk.yummy.tv.feature.posts.navigator.PostDetailsDestination
import su.afk.yummy.tv.feature.posts.navigator.PostsDestination
import javax.inject.Inject

class PostsNavRegistrar @Inject constructor() : IMobilePostsEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<PostsDestination> {
                val vm = hiltViewModel<PostsListViewModel>()
                ScreenNavigator(vm) { state, effect, events ->
                    PostsMobileScreen(
                        state,
                        effect,
                        events
                    )
                }
            }
            entry<PostDetailsDestination> { destination ->
                val vm =
                    hiltViewModel<PostDetailsViewModel, PostDetailsViewModel.Factory>(key = "post-${destination.postId}") {
                        it.create(destination.postId)
                    }
                ScreenNavigator(vm) { state, effect, events ->
                    PostDetailsMobileScreen(
                        state,
                        effect,
                        events
                    )
                }
            }
        }
}
