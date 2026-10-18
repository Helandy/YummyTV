package su.afk.yummy.tv.feature.details.viewingorder.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.tv.TvPosterRating
import su.afk.yummy.tv.core.designsystem.tv.TvPosterYear
import su.afk.yummy.tv.core.designsystem.tv.TvTitleCard
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.bestUrl
import su.afk.yummy.tv.feature.details.view.common.RelatedTitleCardWidth
import su.afk.yummy.tv.feature.details.view.common.TvPosterIndex

@Composable
internal fun ViewingOrderCard(
    index: Int,
    item: AnimeViewingOrderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    val meta = listOfNotNull(
        item.type,
        item.episodesCount?.let { stringResource(R.string.details_episodes_count_short, it) },
    ).joinToString(" · ")

    TvTitleCard(
        title = item.title,
        posterUrl = item.poster?.bestUrl,
        onClick = onClick,
        modifier = modifier,
        subtitle = meta,
        caption = item.relation,
        onFocused = onFocused,
        width = RelatedTitleCardWidth,
        animeId = item.animeId,
        posterOverlay = {
            TvPosterIndex(index)
            item.rating?.let { TvPosterRating(it) }
            item.year?.let { TvPosterYear(it) }
        },
    )
}
