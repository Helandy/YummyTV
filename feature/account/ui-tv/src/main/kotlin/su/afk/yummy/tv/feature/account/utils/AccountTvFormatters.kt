package su.afk.yummy.tv.feature.account.utils

import su.afk.yummy.tv.domain.account.model.UserStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun UserStats.isEmpty(): Boolean =
    genres.isEmpty() && ratings.isEmpty() && lists.isEmpty() && types.isEmpty()

internal fun Long.toDurationLabel(): String {
    val hours = this / 3600L
    val minutes = (this % 3600L) / 60L
    val seconds = this % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

internal fun Long.formatDate(): String =
    if (this <= 0L) "" else SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(this * 1000L))
