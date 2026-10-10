@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.UserGenreStat
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
internal fun GenreStats(topGenres: List<UserGenreStat>) {
    StatSection(title = stringResource(R.string.account_stats_genres)) {
        val max = topGenres.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        topGenres.forEach { genre ->
            StatBar(label = genre.title, valueLabel = genre.count.toString(), fraction = genre.count.toFloat() / max)
        }
    }
}
