package su.afk.yummy.tv.feature.details.mobile.rating.utils

import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary

internal fun AnimeRatingSummary.weightedAverage(): Double? {
    val total = distribution.sumOf { it.count }
    if (total <= 0) return null
    val weighted = distribution.sumOf { it.rating.toDouble() * it.count.toDouble() }
    return weighted / total.toDouble()
}

