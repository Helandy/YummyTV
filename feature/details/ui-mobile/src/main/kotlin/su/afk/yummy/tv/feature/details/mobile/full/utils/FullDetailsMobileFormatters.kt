package su.afk.yummy.tv.feature.details.mobile.full.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val epochSecondsFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

internal fun Long.formatEpochSeconds(): String = epochSecondsFormatter.format(Date(this * 1000))
