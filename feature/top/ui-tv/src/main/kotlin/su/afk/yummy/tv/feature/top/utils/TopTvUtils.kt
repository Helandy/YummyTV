package su.afk.yummy.tv.feature.top.utils

internal fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
