package su.afk.yummy.tv.feature.posts.details.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.posts.tv.R
import java.util.Locale

@Composable
internal fun Int.compactCount(): String {
    // Read both resources unconditionally to keep composition groups stable during prefetch.
    val millions =
        stringResource(R.string.posts_count_millions, (this / 1_000_000f).compactDecimal())
    val thousands =
        stringResource(R.string.posts_count_thousands, (this / 1_000f).compactDecimal())
    return when {
        this >= 1_000_000 -> millions
        this >= 1_000 -> thousands
        else -> toString()
    }
}

private fun Float.compactDecimal(): String =
    if (this % 1f == 0f) toInt().toString() else String.format(Locale.US, "%.1f", this)
