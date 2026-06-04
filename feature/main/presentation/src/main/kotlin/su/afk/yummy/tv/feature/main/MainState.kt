package su.afk.yummy.tv.feature.main

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.navigation.MainMenuFocusTarget
import su.afk.yummy.tv.core.navigation.tab.SideTab
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
        val tvMenu: TvMenuState = TvMenuState(),
    ) : UiState

    data class TvMenuState(
        val currentMenuFocusTarget: MainMenuFocusTarget = MainMenuFocusTarget.SELECTED_TAB,
        val pendingMenuFocusTarget: MainMenuFocusTarget? = null,
        val suppressedFocusSelectionTarget: MainMenuFocusTarget? = null,
        val menuFocusRequestId: Long = 0L,
        val contentFocusRequestId: Long = 0L,
    ) {
        val showSelectedTabBackground: Boolean
            get() = currentMenuFocusTarget == MainMenuFocusTarget.SELECTED_TAB
    }

    sealed class Event : UiEvent {
        data class TvRouteMenuTargetChanged(val target: MainMenuFocusTarget) : Event()
        data class TvMenuTargetFocused(val target: MainMenuFocusTarget) : Event()
        data class TvMenuFocusRequested(val target: MainMenuFocusTarget) : Event()
        data class TvMenuFocusConsumed(val target: MainMenuFocusTarget) : Event()
        data class TvTabFocused(val tab: SideTab) : Event()
        data class TvTabActivated(val tab: SideTab) : Event()
        data object TvSettingsFocused : Event()
        data object TvSettingsActivated : Event()
        data object TvAccountFocused : Event()
        data object TvAccountActivated : Event()
        data object TvContentFocusRequestedFromMenu : Event()
    }

    sealed class Effect : UiEffect {
        data class NavigateToUpdate(
            val version: String,
            val apkUrl: String,
            val changelog: String,
        ) : Effect()
    }
}
