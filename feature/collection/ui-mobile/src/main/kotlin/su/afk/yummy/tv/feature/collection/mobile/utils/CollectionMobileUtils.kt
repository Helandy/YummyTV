package su.afk.yummy.tv.feature.collection.mobile.utils

internal fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
