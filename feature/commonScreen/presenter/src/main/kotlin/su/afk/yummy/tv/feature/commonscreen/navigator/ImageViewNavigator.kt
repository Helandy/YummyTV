package su.afk.yummy.tv.feature.commonscreen.navigator

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.EntryPointAccessors
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.commonscreen.di.ImageViewNavigatorEntryPoint
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

class ImageViewNavigatorRegister @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<CommonScreenDestination.ImageViewDest> { dest ->
                ImageViewEntry(dest)
            }
        }
}

@Composable
private fun ImageViewEntry(dest: CommonScreenDestination.ImageViewDest) {
    val appContext = LocalContext.current.applicationContext
    val entryPoint = EntryPointAccessors.fromApplication(
        appContext,
        ImageViewNavigatorEntryPoint::class.java,
    )
    val factory = entryPoint.imageViewViewModelFactory()

    val vm: ImageViewViewModel = viewModel(
        key = "ImageViewDest:${dest.selectedIndex}:${dest.imageUrls.firstOrNull()}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return factory.create(dest, extras.createSavedStateHandle()) as T
            }
        },
    )

    ScreenNavigator(vm) { state, effect, onEvent ->
        ImageViewScreen(state = state, effect = effect, onEvent = onEvent)
    }
}
