package su.afk.yummy.tv.feature.account.mobile.userprofile.utils

internal fun Int.tabCountLabel(): String =
    if (this > 999) "999+" else coerceAtLeast(0).toString()

internal fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
