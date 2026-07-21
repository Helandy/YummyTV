package su.afk.yummy.tv.feature.search.mobile.utils

internal fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
