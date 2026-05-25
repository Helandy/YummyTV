package su.afk.yummy.tv.feature.main

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.storage.settings.PosterQuality

object MainState {

    data class State(
        val posterQuality: PosterQuality = PosterQuality.STANDARD,
        val showScreenshotsOnFocus: Boolean = false,
        val yaniNickname: String = "",
        val yaniAvatarUrl: String = "",
        val isYaniSignedIn: Boolean = false,
        val unreadNotificationsCount: Int = 0,
    ) : UiState

    sealed class Event : UiEvent

    sealed class Effect : UiEffect {
        data class NavigateToUpdate(
            val version: String,
            val apkUrl: String,
            val changelog: String,
        ) : Effect()
    }
}
