package su.afk.yummy.tv.feature.details.mobile.viewingorder.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterYear
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.core.model.anime.bestUrl
import su.afk.yummy.tv.feature.details.mobile.viewingorder.utils.mobileMeta

@Composable
internal fun ViewingOrderMobileCard(
    index: Int,
    item: AnimeViewingOrderItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    MobilePosterCard(
        title = item.title,
        posterUrl = item.poster.bestUrl(),
        modifier = modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier),
        subtitle = item.mobileMeta(),
        caption = item.relation,
        rating = item.rating,
        animeId = item.animeId,
        posterOverlay = {
            ViewingOrderMobileIndex(index)
            item.year?.let { MobilePosterYear(it) }
        },
        onClick = onClick,
    )
}
