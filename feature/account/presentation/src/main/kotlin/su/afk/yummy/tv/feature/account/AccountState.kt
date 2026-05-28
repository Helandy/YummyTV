package su.afk.yummy.tv.feature.account

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.model.UserStats

const val YANI_HCAPTCHA_SITE_KEY = "b1847961-208e-4a90-9671-1e6bba9e0b36"

class AccountState {
    data class State(
        val accessToken: String = "",
        val userId: Int = 0,
        val nickname: String = "",
        val avatarUrl: String = "",
        val login: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val selectedTab: AccountTab = AccountTab.STATS,
        val stats: UserStats? = null,
        val notifications: List<ProfileNotification> = emptyList(),
        val notificationCounts: List<NotificationCount> = emptyList(),
        val isStatsLoading: Boolean = false,
        val isNotificationsLoading: Boolean = false,
        val isCaptchaRequired: Boolean = false,
        val captchaSiteKey: String = YANI_HCAPTCHA_SITE_KEY,
        val captchaChallengeId: Int = 0,
        val captchaError: String? = null,
        val error: String? = null,
        val hubError: String? = null,
    ) : UiState

    enum class AccountTab {
        STATS,
        NOTIFICATIONS,
    }

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class TabSelected(val tab: AccountTab) : Event
        data class LoginChanged(val login: String) : Event
        data class PasswordChanged(val password: String) : Event
        data object LoginSelected : Event
        data class CaptchaSolved(val token: String) : Event
        data object CaptchaExpired : Event
        data class CaptchaFailed(val message: String? = null) : Event
        data object LogoutSelected : Event
        data object RefreshProfileSelected : Event
        data object RefreshHubSelected : Event
        data class NotificationSelected(val id: Int) : Event
        data class NotificationReadSelected(val id: Int) : Event
        data object AllNotificationsReadSelected : Event
        data class NotificationDeleteSelected(val id: Int) : Event
    }

    sealed interface Effect : UiEffect
}
