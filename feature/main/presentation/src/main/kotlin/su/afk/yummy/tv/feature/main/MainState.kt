package su.afk.yummy.tv.feature.main

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.PosterCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterQuality

object MainState {

    data class State(
        val appTheme: AppTheme = AppTheme.WARM_AMBER,
        val posterQuality: PosterQuality = PosterQuality.STANDARD,
        val posterCardSize: PosterCardSize = PosterCardSize.STANDARD,
        val yaniNickname: String = "",
        val yaniAvatarUrl: String = "",
        val isYaniSignedIn: Boolean = false,
        val isYaniAuthResolved: Boolean = false,
        val unreadNotificationsCount: Int = 0,
    ) : UiState

    /** Пользовательские действия в корневом контейнере приложения. */
    sealed class Event : UiEvent {
        /** Корневая TV вкладка выбрана из меню. */
        data class TvRootSelected(val root: RootTab) : Event()
    }

    sealed class Effect : UiEffect {
        data class NavigateToUpdate(
            val version: String,
            val apkUrl: String,
            val changelog: String,
            val required: Boolean = false,
        ) : Effect()

        data class ShowToast(val message: String) : Effect()
    }
}
