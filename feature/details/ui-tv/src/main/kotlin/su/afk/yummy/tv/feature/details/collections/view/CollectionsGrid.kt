package su.afk.yummy.tv.feature.details.collections.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.collections.utils.posterUrl

@Composable
internal fun CollectionsGrid(
    collections: List<AnimeCollectionSummary>,
    onCollectionSelected: (Int) -> Unit,
) {
    val posterQuality = LocalPosterQuality.current
    val cardWidth = currentTvTitleCardDimensions().width
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = cardWidth),
        contentPadding = PaddingValues(
            start = TvScreenPadding.Horizontal,
            end = TvScreenPadding.Horizontal,
            top = TvScreenPadding.Vertical,
            bottom = TvScreenPadding.Vertical,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.details_collections_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        items(collections, key = { it.id }) { collection ->
            TvTitleCard(
                title = collection.title,
                posterUrl = collection.posterUrl(posterQuality),
                onClick = { onCollectionSelected(collection.id) },
            )
        }
    }
}
