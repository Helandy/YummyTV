package su.afk.yummy.tv.feature.collection.utils

internal fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
