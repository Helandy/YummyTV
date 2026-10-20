package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary

/** Средняя оценка по распределению голосов; `null`, если голосов нет. */
fun AnimeRatingSummary.weightedAverage(): Double? {
    val total = distribution.sumOf { it.count }
    if (total <= 0) return null
    val weighted = distribution.sumOf { it.rating.toDouble() * it.count.toDouble() }
    return weighted / total.toDouble()
}
