package su.afk.yummy.tv.feature.bloggers.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.bloggers.details.BloggerDetailsTvScreen
import su.afk.yummy.tv.feature.bloggers.details.BloggerDetailsViewModel
import su.afk.yummy.tv.feature.bloggers.list.BloggerVideosListTvScreen
import su.afk.yummy.tv.feature.bloggers.list.BloggerVideosListViewModel
import su.afk.yummy.tv.feature.bloggers.navigator.BloggerDetailsDestination
import su.afk.yummy.tv.feature.bloggers.navigator.BloggerVideoDetailsDestination
import su.afk.yummy.tv.feature.bloggers.navigator.BloggerVideosDestination
import su.afk.yummy.tv.feature.bloggers.video.BloggerVideoDetailsTvScreen
import su.afk.yummy.tv.feature.bloggers.video.BloggerVideoDetailsViewModel

class BloggerVideosNavRegistrar : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<BloggerVideosDestination> { destination ->
                val vm =
                    hiltViewModel<BloggerVideosListViewModel, BloggerVideosListViewModel.Factory>(
                        key = "tv-blogger-videos-${destination.animeId}"
                    ) { it.create(destination.animeId) }
                ScreenNavigator(vm) { state, effect, event ->
                    BloggerVideosListTvScreen(
                        state,
                        effect,
                        event
                    )
                }
            }
            entry<BloggerDetailsDestination> { destination ->
                val vm = hiltViewModel<BloggerDetailsViewModel, BloggerDetailsViewModel.Factory>(
                    key = "tv-blogger-${destination.bloggerId}"
                ) { it.create(destination.bloggerId) }
                ScreenNavigator(vm) { state, effect, events ->
                    BloggerDetailsTvScreen(
                        state,
                        effect,
                        events
                    )
                }
            }
            entry<BloggerVideoDetailsDestination> { destination ->
                val vm =
                    hiltViewModel<BloggerVideoDetailsViewModel, BloggerVideoDetailsViewModel.Factory>(
                        key = "tv-blogger-video-${destination.videoId}"
                    ) { it.create(destination.videoId) }
                ScreenNavigator(vm) { state, effect, events ->
                    BloggerVideoDetailsTvScreen(
                        state,
                        effect,
                        events
                    )
                }
            }
        }
}
