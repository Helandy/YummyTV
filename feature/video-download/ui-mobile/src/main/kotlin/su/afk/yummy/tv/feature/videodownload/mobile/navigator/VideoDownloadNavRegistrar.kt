package su.afk.yummy.tv.feature.videodownload.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.videodownload.VideoDownloadViewModel
import su.afk.yummy.tv.feature.videodownload.mobile.VideoDownloadMobileScreen
import su.afk.yummy.tv.feature.videodownload.navigator.VideoDownloadDestination
import javax.inject.Inject

class VideoDownloadNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<VideoDownloadDestination> {
                val viewModel = hiltViewModel<VideoDownloadViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    VideoDownloadMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
