package su.afk.yummy.tv.feature.account.mobile.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.model.AccountUiError

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
        AccountUiError.UPDATE_NOTIFICATION_FAILED -> R.string.account_error_update_notification_failed
        AccountUiError.UPDATE_NOTIFICATIONS_FAILED -> R.string.account_error_update_notifications_failed
    }
