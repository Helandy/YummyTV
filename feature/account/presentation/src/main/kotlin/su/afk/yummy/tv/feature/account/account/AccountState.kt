package su.afk.yummy.tv.feature.account.account

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

const val YANI_HCAPTCHA_SITE_KEY = "b1847961-208e-4a90-9671-1e6bba9e0b36"

class AccountState {
    data class State(
        val isSignedIn: Boolean = false,
        val userId: Int = 0,
        val nickname: String = "",
        val avatarUrl: String = "",
        val login: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val selectedTab: AccountTab = AccountTab.STATS,
        val profileSummary: UserProfileSummary? = null,
        val stats: UserStats? = null,
        val notifications: List<ProfileNotification> = emptyList(),
        val notificationCounts: List<NotificationCount> = emptyList(),
        val isNotificationOpening: Boolean = false,
        val isStatsLoading: Boolean = false,
        val isNotificationsLoading: Boolean = false,
        val isCaptchaRequired: Boolean = false,
        val captchaSiteKey: String = YANI_HCAPTCHA_SITE_KEY,
        val captchaChallengeId: Int = 0,
        val captchaError: AccountUiError? = null,
        val error: AccountUiError? = null,
        val hubError: AccountUiError? = null,
    ) : UiState

    enum class AccountTab {
        STATS,
        NOTIFICATIONS,
    }

    /** Пользовательские действия на экране аккаунта. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь выбрал вкладку аккаунта. */
        data class TabSelected(val tab: AccountTab) : Event

        /** Пользователь изменил логин в форме входа. */
        data class LoginChanged(val login: String) : Event

        /** Пользователь изменил пароль в форме входа. */
        data class PasswordChanged(val password: String) : Event

        /** Пользователь отправил форму входа. */
        data object LoginSelected : Event

        /** Пользователь успешно решил капчу и передал токен проверки. */
        data class CaptchaSolved(val token: String) : Event

        /** Срок действия капчи истёк. */
        data object CaptchaExpired : Event

        /** Проверка капчи завершилась ошибкой с необязательным сообщением. */
        data class CaptchaFailed(val message: String? = null) : Event

        /** Пользователь выбрал выход из аккаунта. */
        data object LogoutSelected : Event

        /** Пользователь запросил обновление профиля. */
        data object RefreshProfileSelected : Event

        /** Пользователь запросил обновление данных вкладок аккаунта. */
        data object RefreshHubSelected : Event

        /** Пользователь открыл уведомление с указанным идентификатором. */
        data class NotificationSelected(val id: Int) : Event

        /** Пользователь отметил уведомление с указанным идентификатором прочитанным. */
        data class NotificationReadSelected(val id: Int) : Event

        /** Пользователь отметил все уведомления прочитанными. */
        data object AllNotificationsReadSelected : Event

        /** Пользователь удалил уведомление с указанным идентификатором. */
        data class NotificationDeleteSelected(val id: Int) : Event
    }

    sealed interface Effect : UiEffect
}
