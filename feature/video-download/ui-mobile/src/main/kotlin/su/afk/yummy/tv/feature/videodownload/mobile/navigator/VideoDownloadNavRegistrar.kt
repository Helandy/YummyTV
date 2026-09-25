package su.afk.yummy.tv.feature.videodownload.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.videodownload.IMobileVideoDownloadEntry
import su.afk.yummy.tv.feature.videodownload.VideoDownloadViewModel
import su.afk.yummy.tv.feature.videodownload.mobile.VideoDownloadMobileScreen
import su.afk.yummy.tv.feature.videodownload.navigator.VideoDownloadDestination
import javax.inject.Inject

class VideoDownloadNavRegistrar @Inject constructor() : IMobileVideoDownloadEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<VideoDownloadDestination> {
                val viewModel = hiltViewModel<VideoDownloadViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    VideoDownloadMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
