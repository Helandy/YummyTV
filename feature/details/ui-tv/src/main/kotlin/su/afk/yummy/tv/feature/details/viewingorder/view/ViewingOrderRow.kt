package su.afk.yummy.tv.feature.details.viewingorder.view

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyListKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.domain.anime.model.AnimeViewingOrderItem
import su.afk.yummy.tv.feature.details.R

private val RelatedCardWidth = 188.dp

@Composable
internal fun ViewingOrderRow(
    items: List<AnimeViewingOrderItem>,
    currentAnimeId: Int,
    onAnimeSelected: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val rowState = rememberLazyListState()
    val itemIds = remember(items) { items.map { it.animeId } }
    val focusRequesters = remember(itemIds) {
        List(items.size) { FocusRequester() }
    }
    val itemFocusRequesters = remember(itemIds, focusRequesters) {
        itemIds.zip(focusRequesters).toMap()
    }
    val focusRestoreState = rememberTvLazyFocusRestoreState<Int>(currentAnimeId)
    val initialFocusIndex = remember(itemIds, currentAnimeId) {
        items.indexOfFirst { it.animeId == currentAnimeId }.takeIf { it >= 0 } ?: 0
    }
    var hasRememberedFocus by rememberSaveable(currentAnimeId) { mutableStateOf(false) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }

    fun rememberFocusedItem(index: Int) {
        itemIds.getOrNull(index)?.let { itemId ->
            hasRememberedFocus = true
            focusRestoreState.onItemFocused(itemId, index)
        }
    }

    fun restoreTargetIndex(): Int {
        if (items.isEmpty()) return 0
        if (!hasRememberedFocus) return initialFocusIndex
        return focusRestoreState.targetIndex(itemIds)?.coerceIn(0, items.lastIndex)
            ?: initialFocusIndex
    }

    fun requestItemFocus(index: Int) {
        if (items.isEmpty()) return
        val target = index.coerceIn(0, items.lastIndex)
        rememberFocusedItem(target)
        restoreFocusJob = launchTvLazyListKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = itemIds,
            listState = rowState,
            itemFocusRequesters = itemFocusRequesters,
            fallbackFocusRequester = focusRequesters.getOrNull(target) ?: FocusRequester.Default,
            fallbackIndex = target,
        )
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    LaunchedEffect(itemIds) {
        if (items.isNotEmpty()) {
            requestItemFocus(restoreTargetIndex())
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
            val sideInset =
                ((maxWidth - RelatedCardWidth) / 2).coerceAtLeast(TvScreenPadding.Horizontal)
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = sideInset),
                horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusRestorer(
                        fallback = focusRequesters.getOrNull(restoreTargetIndex())
                            ?: FocusRequester.Default,
                    ),
            ) {
                itemsIndexed(items, key = { _, item -> item.animeId }) { index, item ->
                    ViewingOrderCard(
                        index = index + 1,
                        item = item,
                        onClick = {
                            rememberFocusedItem(index)
                            onAnimeSelected(item.animeId)
                        },
                        onFocused = { rememberFocusedItem(index) },
                        modifier = Modifier.focusRequester(focusRequesters[index]),
                    )
                }
            }
        }
    }
}
