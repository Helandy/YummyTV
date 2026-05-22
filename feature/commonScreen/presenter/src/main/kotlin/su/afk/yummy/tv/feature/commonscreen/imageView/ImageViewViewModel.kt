package su.afk.yummy.tv.feature.commonscreen.imageView

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.commonscreen.navigator.CommonScreenDestination

internal class ImageViewViewModel @AssistedInject constructor(
    @Assisted private val dest: CommonScreenDestination.ImageViewDest,
    @Assisted savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val navManager: NavigationManager,
) : BaseViewModelNew<ImageViewState.State, ImageViewState.Event, ImageViewState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(
            dest: CommonScreenDestination.ImageViewDest,
            savedStateHandle: SavedStateHandle,
        ): ImageViewViewModel
    }

    override fun createInitialState() = ImageViewState.State(
        images = dest.imageUrls,
        selectedIndex = dest.selectedIndex.coerceIn(0, (dest.imageUrls.size - 1).coerceAtLeast(0)),
    )

    override fun onEvent(event: ImageViewState.Event) {
        when (event) {
            ImageViewState.Event.Back -> navManager.back()
            ImageViewState.Event.Next -> setState {
                copy(selectedIndex = (selectedIndex + 1).coerceAtMost(images.lastIndex))
            }
            ImageViewState.Event.Previous -> setState {
                copy(selectedIndex = (selectedIndex - 1).coerceAtLeast(0))
            }
            is ImageViewState.Event.SelectIndex -> setState {
                copy(selectedIndex = event.index.coerceIn(0, images.lastIndex))
            }
        }
    }
}
