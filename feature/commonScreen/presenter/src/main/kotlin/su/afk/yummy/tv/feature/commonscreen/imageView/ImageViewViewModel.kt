package su.afk.yummy.tv.feature.commonscreen.imageView

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.commonscreen.CommonScreenAnalytics
import su.afk.yummy.tv.feature.commonscreen.R
import su.afk.yummy.tv.feature.commonscreen.handler.ImageSaver
import su.afk.yummy.tv.feature.commonscreen.navigator.CommonScreenDestination

@HiltViewModel(assistedFactory = ImageViewViewModel.Factory::class)
internal class ImageViewViewModel @AssistedInject constructor(
    @Assisted private val dest: CommonScreenDestination.ImageViewDest,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val navManager: INavigationManager,
    private val analytics: CommonScreenAnalytics,
    private val imageSaver: ImageSaver,
) : BaseViewModel<ImageViewState.State, ImageViewState.Event, ImageViewState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(
            dest: CommonScreenDestination.ImageViewDest,
        ): ImageViewViewModel
    }

    override fun createInitialState() = ImageViewState.State(
        images = dest.imageUrls.toImmutableList(),
        selectedIndex = dest.selectedIndex.coerceIn(0, (dest.imageUrls.size - 1).coerceAtLeast(0)),
    )

    override fun onEvent(event: ImageViewState.Event) {
        when (event) {
            ImageViewState.Event.Back -> navManager.back()
            ImageViewState.Event.Next -> {
                analytics.eventImageNext(currentState.images.size, currentState.selectedIndex)
                setState {
                    copy(selectedIndex = (selectedIndex + 1).coerceAtMost(images.lastIndex))
                }
            }

            ImageViewState.Event.Previous -> {
                analytics.eventImagePrevious(currentState.images.size, currentState.selectedIndex)
                setState {
                    copy(selectedIndex = (selectedIndex - 1).coerceAtLeast(0))
                }
            }

            ImageViewState.Event.Download -> download()

            is ImageViewState.Event.SelectIndex -> {
                analytics.eventImageSelectIndex(
                    imageCount = currentState.images.size,
                    selectedIndex = event.index.coerceIn(0, currentState.images.lastIndex),
                )
                setState {
                    copy(selectedIndex = event.index.coerceIn(0, images.lastIndex))
                }
            }
        }
    }

    private fun download() {
        val url = currentState.currentImage ?: return
        if (currentState.isDownloading) return
        analytics.eventImageDownload(currentState.images.size, currentState.selectedIndex)
        setState { copy(isDownloading = true) }
        viewModelScope.launch {
            val result = imageSaver.save(url)
            setState { copy(isDownloading = false) }
            setEffect(
                ImageViewState.Effect.ShowToast(
                    UiText.Res(
                        if (result.isSuccess) R.string.image_saved else R.string.image_save_failed,
                    ),
                ),
            )
        }
    }
}
