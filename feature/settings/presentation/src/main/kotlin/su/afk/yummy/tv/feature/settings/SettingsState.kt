package su.afk.yummy.tv.feature.settings

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.storage.settings.PosterQuality
import su.afk.yummy.tv.core.storage.settings.PreferredPlayer
import su.afk.yummy.tv.core.storage.settings.PreviewCacheSize

class SettingsState {
    data class State(
        val posterQuality: PosterQuality = PosterQuality.STANDARD,
        val showScreenshotsOnFocus: Boolean = false,
        val preferredPlayer: PreferredPlayer = PreferredPlayer.NONE,
        val isPreviewChannelBrowsable: Boolean = false,
        val watchNextEnabled: Boolean = true,
        val previewCacheSize: PreviewCacheSize = PreviewCacheSize.MB_100,
        val autoSkipOpeningsEndings: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class PosterQualitySelected(val quality: PosterQuality) : Event
        data object ShowScreenshotsOnFocusToggled : Event
        data class PreferredPlayerSelected(val player: PreferredPlayer) : Event
        data object RequestPreviewChannelBrowsable : Event
        data object WatchNextToggled : Event
        data class PreviewCacheSizeSelected(val size: PreviewCacheSize) : Event
        data object AutoSkipOpeningsEndingsToggled : Event
    }

    sealed interface Effect : UiEffect
}
