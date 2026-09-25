package su.afk.yummy.tv.feature.posts.mobile.navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Newspaper
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
import su.afk.yummy.tv.feature.posts.IMobilePostsEntry
import su.afk.yummy.tv.feature.posts.details.PostDetailsViewModel
import su.afk.yummy.tv.feature.posts.list.PostsListViewModel
import su.afk.yummy.tv.feature.posts.mobile.R
import su.afk.yummy.tv.feature.posts.mobile.details.PostDetailsMobileScreen
import su.afk.yummy.tv.feature.posts.mobile.list.PostsMobileScreen
import su.afk.yummy.tv.feature.posts.navigator.PostDetailsDestination
import su.afk.yummy.tv.feature.posts.navigator.PostsDestination
import javax.inject.Inject

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class PostsNavRegistrar @Inject constructor() : IMobilePostsEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<PostsDestination>(metadata = listPaneMetadata()) {
                val vm = hiltViewModel<PostsListViewModel>()
                ScreenNavigator(vm) { state, effect, events ->
                    PostsMobileScreen(
                        state,
                        effect,
                        events
                    )
                }
            }
            entry<PostDetailsDestination>(metadata = ListDetailSceneStrategy.detailPane(LIST_DETAIL_SCENE_KEY)) { destination ->
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

    private fun listPaneMetadata() = listPaneAnchor() + ListDetailSceneStrategy.listPane(
        sceneKey = LIST_DETAIL_SCENE_KEY,
        detailPlaceholder = {
            MobileDetailPlaceholder(
                icon = Icons.Outlined.Newspaper,
                text = stringResource(R.string.posts_mobile_detail_placeholder),
            )
        },
    )
}

private const val LIST_DETAIL_SCENE_KEY = "posts"
