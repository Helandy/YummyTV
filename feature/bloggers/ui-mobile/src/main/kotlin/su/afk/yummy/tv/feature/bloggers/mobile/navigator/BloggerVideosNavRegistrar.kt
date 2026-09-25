package su.afk.yummy.tv.feature.bloggers.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.bloggers.IMobileBloggerVideosEntry
import su.afk.yummy.tv.feature.bloggers.details.BloggerDetailsViewModel
import su.afk.yummy.tv.feature.bloggers.list.BloggerVideosListViewModel
import su.afk.yummy.tv.feature.bloggers.mobile.details.BloggerDetailsMobileScreen
import su.afk.yummy.tv.feature.bloggers.mobile.list.BloggerVideosListMobileScreen
import su.afk.yummy.tv.feature.bloggers.mobile.video.BloggerVideoDetailsMobileScreen
import su.afk.yummy.tv.feature.bloggers.navigator.BloggerDetailsDestination
import su.afk.yummy.tv.feature.bloggers.navigator.BloggerVideoDetailsDestination
import su.afk.yummy.tv.feature.bloggers.navigator.BloggerVideosDestination
import su.afk.yummy.tv.feature.bloggers.video.BloggerVideoDetailsViewModel
import javax.inject.Inject

class BloggerVideosNavRegistrar @Inject constructor() : IMobileBloggerVideosEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
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
