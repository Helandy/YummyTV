package su.afk.yummy.tv.feature.commonscreen.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.commonscreen.imageView.ImageViewScreen
import su.afk.yummy.tv.feature.commonscreen.imageView.ImageViewViewModel
import javax.inject.Inject

class ImageViewNavigator @Inject constructor() : IImageViewNavigator {
    override fun invoke(
        imageUrl: String,
        imageUrls: List<String>,
        selectedIndex: Int?,
        service: String?,
        creatorName: String?,
        postId: String?,
        postTitle: String?,
        thumbnailUrls: Map<String, String>,
    ): NavKey {
        val allUrls = imageUrls.ifEmpty { listOf(imageUrl) }
        val index = selectedIndex ?: allUrls.indexOf(imageUrl).coerceAtLeast(0)
        return CommonScreenDestination.ImageViewDest(
            imageUrls = allUrls,
            selectedIndex = index,
        )
    }
}

class ImageViewNavigatorRegister @Inject constructor() : IImageViewEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<CommonScreenDestination.ImageViewDest> { dest ->
                val vm = hiltViewModel<ImageViewViewModel, ImageViewViewModel.Factory>(
                    key = "ImageViewDest:${dest.selectedIndex}:${dest.imageUrls.firstOrNull()}",
                ) { it.create(dest) }
                ScreenNavigator(vm) { state, effect, onEvent ->
                    ImageViewScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
