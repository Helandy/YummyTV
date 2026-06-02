package su.afk.yummy.tv.feature.settings

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.PreviewCacheSize
import su.afk.yummy.tv.core.preferences.settings.SettingsStore

enum class DetailsButtonMoveDirection {
    UP,
    DOWN,
}

class SettingsState {
    data class State(
        val appTheme: AppTheme = AppTheme.WARM_AMBER,
        val posterQuality: PosterQuality = PosterQuality.STANDARD,
        val showScreenshotsOnFocus: Boolean = false,
        val preferredPlayer: PreferredPlayer = PreferredPlayer.NONE,
        val isPreviewChannelBrowsable: Boolean = false,
        val tvHomeIntegrationSupported: Boolean = true,
        val watchNextEnabled: Boolean = true,
        val previewCacheSize: PreviewCacheSize = PreviewCacheSize.MB_100,
        val autoSkipOpeningsEndings: Boolean = false,
        val yaniApplicationToken: String = "",
        val detailsButtonOrder: List<DetailsButtonAction> = SettingsStore.defaultDetailsButtonOrder,
    ) : UiState

    sealed interface Event : UiEvent {
        data class AppThemeSelected(val theme: AppTheme) : Event
        data class PosterQualitySelected(val quality: PosterQuality) : Event
        data object ShowScreenshotsOnFocusToggled : Event
        data class PreferredPlayerSelected(val player: PreferredPlayer) : Event
        data object RequestPreviewChannelBrowsable : Event
        data object WatchNextToggled : Event
        data class PreviewCacheSizeSelected(val size: PreviewCacheSize) : Event
        data object AutoSkipOpeningsEndingsToggled : Event
        data class YaniApplicationTokenChanged(val token: String) : Event
        data class DetailsButtonMoved(
            val action: DetailsButtonAction,
            val direction: DetailsButtonMoveDirection,
        ) : Event
        data object DetailsButtonOrderReset : Event
    }

    sealed interface Effect : UiEffect
}
