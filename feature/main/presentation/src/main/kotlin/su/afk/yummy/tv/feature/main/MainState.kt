package su.afk.yummy.tv.feature.main

import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.core.navigation.root.RootTab

class MainState {

    data class State(
        val appTheme: AppTheme = AppTheme.WARM_AMBER,
        val backgroundStyle: BackgroundStyle = BackgroundStyle.DARK,
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
        /**
         * Корневая вкладка выбрана в меню (боковое на TV, бар или рейка на мобильном).
         * [popToRootOnReselect] — сбросить стек вкладки при повторном выборе текущей.
         */
        data class RootSelected(val root: RootTab, val popToRootOnReselect: Boolean) : Event()
    }

    sealed class Effect : UiEffect {
        /** Глобальный тост поверх экранов. Эффект, а не state: не всплывает повторно. */
        data class ShowToast(val message: String) : Effect()
    }
}
