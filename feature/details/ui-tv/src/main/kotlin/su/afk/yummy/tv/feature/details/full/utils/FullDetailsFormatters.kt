package su.afk.yummy.tv.feature.details.full.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun Long.formatEpochSeconds(): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(this * 1000))
}
