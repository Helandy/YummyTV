package su.afk.yummy.tv.feature.commonscreen.imageView

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState

internal class ImageViewState {

    data class State(
        val images: List<String> = emptyList(),
        val selectedIndex: Int = 0,
    ) : UiState {
        val currentImage: String? get() = images.getOrNull(selectedIndex)
        val hasPrevious: Boolean get() = selectedIndex > 0
        val hasNext: Boolean get() = selectedIndex < images.lastIndex
    }

    sealed interface Event : UiEvent {
        data object Back : Event
        data object Next : Event
        data object Previous : Event
        data class SelectIndex(val index: Int) : Event
    }

    sealed interface Effect : UiEffect
}
