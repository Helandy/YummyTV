package su.afk.yummy.tv.feature.comments.mobile.utils

internal fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
