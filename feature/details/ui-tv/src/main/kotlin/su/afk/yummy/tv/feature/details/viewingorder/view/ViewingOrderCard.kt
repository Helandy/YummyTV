package su.afk.yummy.tv.feature.details.viewingorder.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.anime.model.AnimeViewingOrderItem
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.utils.bestUrl
import su.afk.yummy.tv.feature.details.view.common.RelatedTitleCard

@Composable
internal fun ViewingOrderCard(
    index: Int,
    item: AnimeViewingOrderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    val meta = listOfNotNull(
        item.year?.toString(),
        item.type,
        item.episodesCount?.let { stringResource(R.string.details_episodes_count_short, it) },
    ).joinToString(" · ")

    RelatedTitleCard(
        title = item.title,
        posterUrl = item.poster?.bestUrl,
        onClick = onClick,
        modifier = modifier,
        index = index,
        rating = item.rating,
        relation = item.relation,
        meta = meta,
        onFocused = onFocused,
    )
}
