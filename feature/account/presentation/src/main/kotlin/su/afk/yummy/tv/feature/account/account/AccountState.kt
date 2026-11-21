package su.afk.yummy.tv.feature.account.account

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.utils.YANI_HCAPTCHA_SITE_KEY

class AccountState {
    @Immutable
    data class State(
        /** false, пока не пришёл первый снапшот сессии: экран ещё не знает, авторизован ли пользователь. */
        val isSessionResolved: Boolean = false,
        val isSignedIn: Boolean = false,
        /** Пункты аккаунта показываются отдельными кнопками, а не сгруппированы (настройка). */
        val userId: Int = 0,
        val nickname: String = "",
        val avatarUrl: String = "",
        val login: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val isLogoutInProgress: Boolean = false,
        val selectedTab: AccountTab = AccountTab.STATS,
        val profileSummary: UserProfileSummary? = null,
        val stats: UserStats? = null,
        /** Число непрочитанных уведомлений для значка на кнопке; сам список ведёт NotificationsViewModel. */
        val unreadNotificationCount: Int = 0,
        val isStatsLoading: Boolean = false,
        val isCaptchaRequired: Boolean = false,
        val captchaSiteKey: String = YANI_HCAPTCHA_SITE_KEY,
        val captchaChallengeId: Int = 0,
        val captchaError: AccountUiError? = null,
        val error: AccountUiError? = null,
        val errorMessage: String? = null,
        val hubError: AccountUiError? = null,
        val localAuthServerState: LocalAuthServerState = LocalAuthServerState.Idle,
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

        /** Пользователь открыл скачанные серии на устройстве. */
        data object DownloadedEpisodesSelected : Event


        /** Экран показан — перечитываем данные профиля, если их кэш успел устареть. */
        data object ScreenShown : Event

        /** Переход к FAQ из быстрых действий аккаунта. */
        data object FaqSelected : Event

        /** Переход к информационным страницам сайта из быстрых действий аккаунта. */
        data object SitePagesSelected : Event

        /** Переход к настройкам приложения из быстрых действий аккаунта. */
        data object SettingsSelected : Event

        /** Открыть список уведомлений отдельным экраном. */
        data object NotificationsSelected : Event
        data object ProfileEditSelected : Event
        data object PasswordResetSelected : Event
        data object RegistrationSelected : Event

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

        /** Попытка не удалась — сбросить введённые данные, чтобы менеджер паролей не предложил их сохранить. */
        data object DiscardAutofill : Effect
    }
}
