package su.afk.yummy.tv.feature.main

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.PosterQuality

object MainState {

    data class State(
        val appTheme: AppTheme = AppTheme.WARM_AMBER,
        val posterQuality: PosterQuality = PosterQuality.STANDARD,
        val showScreenshotsOnFocus: Boolean = false,
        val yaniNickname: String = "",
        val yaniAvatarUrl: String = "",
        val isYaniSignedIn: Boolean = false,
        val unreadNotificationsCount: Int = 0,
    ) : UiState

    sealed class Event : UiEvent {
        data class TvRootFocused(val root: RootTab) : Event()
    }

    sealed class Effect : UiEffect {
        data class NavigateToUpdate(
            val version: String,
            val apkUrl: String,
            val changelog: String,
        ) : Effect()
    }
}
