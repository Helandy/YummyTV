package su.afk.yummy.tv.feature.details.collections.utils

import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary

internal fun AnimeCollectionSummary.posterUrl(quality: PosterQuality): String? = when (quality) {
    PosterQuality.LOW -> poster?.medium ?: poster?.big ?: poster?.fullsize ?: poster?.small ?: posterUrl
    PosterQuality.STANDARD -> poster?.big ?: poster?.medium ?: poster?.fullsize ?: poster?.small ?: posterUrl
    PosterQuality.MEGA -> poster?.mega ?: poster?.big ?: poster?.medium ?: poster?.fullsize ?: poster?.small ?: posterUrl
    PosterQuality.HIGH -> poster?.fullsize ?: poster?.mega ?: poster?.big ?: poster?.medium ?: poster?.small ?: posterUrl
}
