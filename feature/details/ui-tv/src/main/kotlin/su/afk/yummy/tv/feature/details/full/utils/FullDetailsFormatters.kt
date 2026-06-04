package su.afk.yummy.tv.feature.details.full.utils

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun Long.formatEpochSeconds(): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(this * 1000))
}

internal fun Double.toYaniRatingColor(): Color = when {
    this < 7.0 -> Color(0xFFE53935)
    this <= 9.0 -> Color(0xFFFFC857)
    else -> Color(0xFF69F0AE)
}
