package su.afk.yummy.tv.feature.bloggers.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.bloggers.details.BloggerDetailsViewModel
import su.afk.yummy.tv.feature.bloggers.list.BloggerVideosListViewModel
import su.afk.yummy.tv.feature.bloggers.mobile.details.BloggerDetailsMobileScreen
import su.afk.yummy.tv.feature.bloggers.mobile.list.BloggerVideosListMobileScreen
import su.afk.yummy.tv.feature.bloggers.mobile.video.BloggerVideoDetailsMobileScreen
import su.afk.yummy.tv.feature.bloggers.navigator.BloggerDetailsDestination
import su.afk.yummy.tv.feature.bloggers.navigator.BloggerVideoDetailsDestination
import su.afk.yummy.tv.feature.bloggers.navigator.BloggerVideosDestination
import su.afk.yummy.tv.feature.bloggers.video.BloggerVideoDetailsViewModel

class BloggerVideosNavRegistrar : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<BloggerVideosDestination> { destination ->
                val vm =
                    hiltViewModel<BloggerVideosListViewModel, BloggerVideosListViewModel.Factory>(
                        key = "blogger-videos-${destination.animeId}"
                    ) { it.create(destination.animeId) }
                ScreenNavigator(vm) { state, effect, event ->
                    BloggerVideosListMobileScreen(
                        state,
                        effect,
                        event
                    )
                }
            }
            entry<BloggerDetailsDestination> { destination ->
                val vm = hiltViewModel<BloggerDetailsViewModel, BloggerDetailsViewModel.Factory>(
                    key = "blogger-${destination.bloggerId}"
                ) { it.create(destination.bloggerId) }
                ScreenNavigator(vm) { state, effect, events ->
                    BloggerDetailsMobileScreen(
                        state,
                        effect,
                        events
                    )
                }
            }
            entry<BloggerVideoDetailsDestination> { destination ->
                val vm =
                    hiltViewModel<BloggerVideoDetailsViewModel, BloggerVideoDetailsViewModel.Factory>(
                        key = "blogger-video-${destination.videoId}"
                    ) { it.create(destination.videoId) }
                ScreenNavigator(vm) { state, effect, events ->
                    BloggerVideoDetailsMobileScreen(
                        state,
                        effect,
                        events
                    )
                }
            }
        }
}
