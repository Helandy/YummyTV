package su.afk.yummy.tv.feature.account.localauth

import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.utils.analyticsParamsOf
import su.afk.yummy.tv.domain.account.model.LocalAuthError
import javax.inject.Inject

/**
 * События входа на ТВ через телефон.
 *
 * В трекер не уходят PIN, refresh-токен, адреса и имена найденных устройств: это либо секреты,
 * либо данные локальной сети пользователя. Только исходы и обезличенные счётчики.
 */
internal class LocalAuthAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {

    // --- ТВ: сторона, показывающая PIN ---

    /** Пользователь выбрал «Войти с телефона» на ТВ. */
    fun eventTvPairingStarted() {
        tracker.track(EVENT_TV_PAIRING_STARTED)
    }

    /** Сервис объявлен в сети, PIN на экране — сопряжение реально доступно. */
    fun eventTvPinShown() {
        tracker.track(EVENT_TV_PIN_SHOWN)
    }

    /** Пользователь запросил новый код после истечения срока или лимита попыток. */
    fun eventTvPinRefreshed() {
        tracker.track(EVENT_TV_PIN_REFRESHED)
    }

    /**
     * С телефона пришёл неверный код.
     *
     * Параметры: attempts_left.
     */
    fun eventTvWrongPin(attemptsLeft: Int) {
        tracker.track(
            EVENT_TV_WRONG_PIN,
            analyticsParamsOf(PARAM_ATTEMPTS_LEFT to attemptsLeft),
        )
    }

    /** Сессия принята, ТВ авторизован. */
    fun eventTvTransferSuccess() {
        tracker.track(EVENT_TV_TRANSFER_SUCCESS)
    }

    /**
     * Сопряжение прекращено: код истёк, кончились попытки, не удалось объявить сервис или войти.
     *
     * Параметры: reason.
     */
    fun eventTvPairingFailed(reason: LocalAuthError) {
        tracker.track(EVENT_TV_PAIRING_FAILED, analyticsParamsOf(PARAM_REASON to reason.param()))
    }

    /** Пользователь закрыл панель сопряжения сам. */
    fun eventTvPairingCancelled() {
        tracker.track(EVENT_TV_PAIRING_CANCELLED)
    }

    // --- Телефон: сторона, передающая сессию ---

    /** Открыт экран «Войти на ТВ». */
    fun eventMobileScreenOpened() {
        tracker.track(EVENT_MOBILE_SCREEN_OPENED)
    }

    /**
     * Итог запроса разрешения на поиск устройств в локальной сети.
     *
     * Параметры: granted.
     */
    fun eventMobilePermissionResult(granted: Boolean) {
        tracker.track(
            EVENT_MOBILE_PERMISSION_RESULT,
            analyticsParamsOf(PARAM_GRANTED to granted),
        )
    }

    /**
     * Сколько устройств нашлось за время работы экрана. Ноль — самый интересный случай:
     * обычно это изоляция клиентов на роутере или разные сети.
     *
     * Параметры: device_count.
     */
    fun eventMobileDiscoveryFinished(deviceCount: Int) {
        tracker.track(
            EVENT_MOBILE_DISCOVERY_FINISHED,
            analyticsParamsOf(PARAM_DEVICE_COUNT to deviceCount),
        )
    }

    /** Пользователь выбрал ТВ из списка. */
    fun eventMobileDeviceSelected() {
        tracker.track(EVENT_MOBILE_DEVICE_SELECTED)
    }

    /** Пользователь нажал «Передать сессию». */
    fun eventMobileTransferSelected() {
        tracker.track(EVENT_MOBILE_TRANSFER_SELECTED)
    }

    /** Телефон получил подтверждение от ТВ. */
    fun eventMobileTransferSuccess() {
        tracker.track(EVENT_MOBILE_TRANSFER_SUCCESS)
    }

    /**
     * Передача не удалась. `reason == null` — сетевой сбой, ТВ ничего не ответил.
     *
     * Параметры: reason.
     */
    fun eventMobileTransferFailure(reason: LocalAuthError?) {
        tracker.track(
            EVENT_MOBILE_TRANSFER_FAILURE,
            analyticsParamsOf(PARAM_REASON to reason.param()),
        )
    }

    /** Поиск устройств не запустился — обычно отозванное разрешение или выключенный Wi-Fi. */
    fun eventMobileDiscoveryFailed() {
        tracker.track(EVENT_MOBILE_DISCOVERY_FAILED)
    }

    private fun LocalAuthError?.param(): String = this?.name?.lowercase() ?: REASON_UNKNOWN

    internal companion object {
        private const val PARAM_REASON = "reason"
        private const val PARAM_ATTEMPTS_LEFT = "attempts_left"
        private const val PARAM_GRANTED = "granted"
        private const val PARAM_DEVICE_COUNT = "device_count"
        private const val REASON_UNKNOWN = "unknown"

        const val EVENT_TV_PAIRING_STARTED = "local_auth_tv_pairing_started"
        const val EVENT_TV_PIN_SHOWN = "local_auth_tv_pin_shown"
        const val EVENT_TV_PIN_REFRESHED = "local_auth_tv_pin_refreshed"
        const val EVENT_TV_WRONG_PIN = "local_auth_tv_wrong_pin"
        const val EVENT_TV_TRANSFER_SUCCESS = "local_auth_tv_transfer_success"
        const val EVENT_TV_PAIRING_FAILED = "local_auth_tv_pairing_failed"
        const val EVENT_TV_PAIRING_CANCELLED = "local_auth_tv_pairing_cancelled"

        const val EVENT_MOBILE_SCREEN_OPENED = "local_auth_mobile_screen"
        const val EVENT_MOBILE_PERMISSION_RESULT = "local_auth_mobile_permission_result"
        const val EVENT_MOBILE_DISCOVERY_FINISHED = "local_auth_mobile_discovery_finished"
        const val EVENT_MOBILE_DISCOVERY_FAILED = "local_auth_mobile_discovery_failed"
        const val EVENT_MOBILE_DEVICE_SELECTED = "local_auth_mobile_device_selected"
        const val EVENT_MOBILE_TRANSFER_SELECTED = "local_auth_mobile_transfer_selected"
        const val EVENT_MOBILE_TRANSFER_SUCCESS = "local_auth_mobile_transfer_success"
        const val EVENT_MOBILE_TRANSFER_FAILURE = "local_auth_mobile_transfer_failure"
    }
}
