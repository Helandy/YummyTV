package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.domain.anime.model.AnimeViewingOrderItem
import su.afk.yummy.tv.feature.details.R

private val RelatedCardWidth = 188.dp

@Composable
internal fun ViewingOrderRow(
    items: List<AnimeViewingOrderItem>,
    currentAnimeId: Int,
    onAnimeSelected: (Int) -> Unit,
) {
    val rowState = rememberLazyListState()
    val focusRequesters = remember(items.map { it.animeId }) {
        List(items.size) { FocusRequester() }
    }
    val initialFocusIndex = remember(items, currentAnimeId) {
        items.indexOfFirst { it.animeId == currentAnimeId }.takeIf { it >= 0 } ?: 0
    }
    LaunchedEffect(initialFocusIndex, items.size) {
        if (focusRequesters.isNotEmpty()) {
            rowState.scrollToItem(initialFocusIndex)
            runCatching { focusRequesters[initialFocusIndex].requestFocus() }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = TvScreenPadding.Horizontal),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.details_viewing_order_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.details_viewing_order_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sideInset = ((maxWidth - RelatedCardWidth) / 2).coerceAtLeast(TvScreenPadding.Horizontal)
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = sideInset),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(items, key = { _, item -> item.animeId }) { index, item ->
                    ViewingOrderCard(
                        index = index + 1,
                        item = item,
                        onClick = { onAnimeSelected(item.animeId) },
                        modifier = Modifier.focusRequester(focusRequesters[index]),
                    )
                }
            }
        }
    }
}
