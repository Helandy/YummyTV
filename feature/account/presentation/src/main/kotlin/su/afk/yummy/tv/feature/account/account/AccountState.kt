package su.afk.yummy.tv.feature.account.account

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

const val YANI_HCAPTCHA_SITE_KEY = "b1847961-208e-4a90-9671-1e6bba9e0b36"

class AccountState {
    @Immutable
    data class State(
        /** false, пока не пришёл первый снапшот сессии: экран ещё не знает, авторизован ли пользователь. */
        val isSessionResolved: Boolean = false,
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
        val notifications: ImmutableList<ProfileNotification> = persistentListOf(),
        val notificationCounts: ImmutableList<NotificationCount> = persistentListOf(),
        val isNotificationOpening: Boolean = false,
        val isStatsLoading: Boolean = false,
        val isNotificationsLoading: Boolean = false,
        val isCaptchaRequired: Boolean = false,
        val captchaSiteKey: String = YANI_HCAPTCHA_SITE_KEY,
        val captchaChallengeId: Int = 0,
        val captchaError: AccountUiError? = null,
        val error: AccountUiError? = null,
        val errorMessage: String? = null,
        val hubError: AccountUiError? = null,
        val localAuthServerState: LocalAuthServerState = LocalAuthServerState.Idle,
    ) : UiState {
        val unreadNotificationCounts: ImmutableList<NotificationCount>
            get() = notificationCounts.filterNot { it.type.equals("message", ignoreCase = true) }
                .toImmutableList()

        val unreadNotificationCount: Int
            get() = unreadNotificationCounts.sumOf { it.count }
    }

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

        /** Пользователь открыл скачанные серии на устройстве. */
        data object DownloadedEpisodesSelected : Event

        data object WatchLaterSelected : Event
        data object MessagesSelected : Event

        /** Экран показан — перечитываем уведомления, если их кэш успел устареть. */
        data object ScreenShown : Event

        data object UserSearchSelected : Event

        /** Переход к списку подписок пользователя. */
        data object MySubscriptionsSelected : Event
        data object ProfileEditSelected : Event
        data object PasswordResetSelected : Event
        data object RegistrationSelected : Event

        /** Пользователь открыл уведомление с указанным идентификатором. */
        data class NotificationSelected(val id: Int) : Event

        /** Пользователь отметил уведомление с указанным идентификатором прочитанным. */
        data class NotificationReadSelected(val id: Int) : Event

        /** Пользователь отметил все уведомления прочитанными. */
        data object AllNotificationsReadSelected : Event

        /** Пользователь удалил все уведомления. */
        data object AllNotificationsDeleteSelected : Event

        /** Пользователь удалил уведомление с указанным идентификатором. */
        data class NotificationDeleteSelected(val id: Int) : Event

        /** ТВ: поднять локальный сервер и показать PIN для передачи сессии с телефона. */
        data object StartLocalAuthServerSelected : Event
        data object StopLocalAuthServerSelected : Event

        /** ТВ: перевыпустить PIN после истечения срока или исчерпания попыток. */
        data object RefreshLocalAuthPinSelected : Event

        /** ТВ: пользователь не дал разрешение на работу в локальной сети — сервер не поднимаем. */
        data object LocalAuthPermissionDenied : Event
    }

    sealed interface Effect : UiEffect {
        data object ShowCaptchaHint : Effect
        data object HideKeyboard : Effect
    }
}
