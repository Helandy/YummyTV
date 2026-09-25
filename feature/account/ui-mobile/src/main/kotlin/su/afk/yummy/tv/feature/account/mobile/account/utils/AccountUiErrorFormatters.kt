package su.afk.yummy.tv.feature.account.mobile.account.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun AccountUiError?.accountErrorMessage(): String? =
    this?.let { stringResource(it.messageRes) }

@get:StringRes
private val AccountUiError.messageRes: Int
    get() = when (this) {
        AccountUiError.CAPTCHA_RESPONSE_EMPTY -> R.string.account_error_captcha_response_empty
        AccountUiError.CAPTCHA_EXPIRED -> R.string.account_error_captcha_expired
        AccountUiError.CAPTCHA_LOAD_FAILED -> R.string.account_error_captcha_load_failed
        AccountUiError.CAPTCHA_REJECTED -> R.string.account_error_captcha_rejected
        AccountUiError.CREDENTIALS_REQUIRED -> R.string.account_error_credentials_required
        AccountUiError.LOGOUT_FAILED -> R.string.account_error_logout_failed
        AccountUiError.REFRESH_FAILED -> R.string.account_error_refresh_failed
        AccountUiError.OPEN_NOTIFICATION_FAILED -> R.string.account_error_open_notification_failed
        AccountUiError.SIGN_IN_FAILED -> R.string.account_error_sign_in_failed
        AccountUiError.LOAD_PROFILE_STATISTICS_FAILED -> R.string.account_error_load_profile_statistics_failed
        AccountUiError.LOAD_NOTIFICATIONS_FAILED -> R.string.account_error_load_notifications_failed
        AccountUiError.LOAD_SUBSCRIPTIONS_FAILED -> R.string.account_error_load_subscriptions_failed
        AccountUiError.UPDATE_NOTIFICATION_FAILED -> R.string.account_error_update_notification_failed
        AccountUiError.UPDATE_NOTIFICATIONS_FAILED -> R.string.account_error_update_notifications_failed
        AccountUiError.REGISTRATION_FAILED -> R.string.account_error_registration_failed
        AccountUiError.INVALID_EMAIL -> R.string.account_error_invalid_email
        AccountUiError.PASSWORD_TOO_SHORT -> R.string.account_error_password_too_short
        AccountUiError.TRANSFER_FAILED -> R.string.account_error_transfer_failed
        AccountUiError.LOCAL_AUTH_INVALID_PIN -> R.string.account_error_local_auth_invalid_pin
        AccountUiError.LOCAL_AUTH_PIN_EXPIRED -> R.string.account_error_local_auth_pin_expired
        AccountUiError.LOCAL_AUTH_TOO_MANY_ATTEMPTS -> R.string.account_error_local_auth_too_many_attempts
        AccountUiError.LOCAL_AUTH_SIGN_IN_FAILED -> R.string.account_error_local_auth_sign_in_failed
        AccountUiError.LOCAL_AUTH_INVALID_QR -> R.string.account_error_local_auth_invalid_qr
        AccountUiError.LOCAL_AUTH_SCANNER_UNAVAILABLE -> R.string.account_error_local_auth_scanner_unavailable
    }
