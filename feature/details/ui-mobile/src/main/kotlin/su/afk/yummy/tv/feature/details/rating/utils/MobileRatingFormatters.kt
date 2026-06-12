package su.afk.yummy.tv.feature.details.rating.utils

import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import java.util.Locale

internal fun AnimeRatingSummary.weightedAverage(): Double? {
    val total = distribution.sumOf { it.count }
    if (total <= 0) return null
    val weighted = distribution.sumOf { it.rating.toDouble() * it.count.toDouble() }
    return weighted / total.toDouble()
}

internal fun Double.formatRating(): String = String.format(Locale.US, "%.1f", this)
