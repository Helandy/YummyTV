package su.afk.yummy.tv.feature.account.localauth.mapper

import su.afk.yummy.tv.domain.account.model.LocalAuthError
import su.afk.yummy.tv.domain.account.model.SessionTransferException
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

internal fun Throwable.toUiError(): AccountUiError =
    when ((this as? SessionTransferException)?.reason) {
        LocalAuthError.INVALID_PIN -> AccountUiError.LOCAL_AUTH_INVALID_PIN
        LocalAuthError.PIN_EXPIRED -> AccountUiError.LOCAL_AUTH_PIN_EXPIRED
        LocalAuthError.TOO_MANY_ATTEMPTS -> AccountUiError.LOCAL_AUTH_TOO_MANY_ATTEMPTS
        LocalAuthError.SIGN_IN_FAILED -> AccountUiError.LOCAL_AUTH_SIGN_IN_FAILED
        else -> AccountUiError.TRANSFER_FAILED
    }
