package su.afk.yummy.tv.feature.comments.utils

internal fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
