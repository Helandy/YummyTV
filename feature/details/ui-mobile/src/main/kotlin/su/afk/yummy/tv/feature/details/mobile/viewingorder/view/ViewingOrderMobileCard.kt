package su.afk.yummy.tv.feature.details.mobile.viewingorder.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterType
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterYear
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.core.model.anime.urlsByQuality

@Composable
internal fun ViewingOrderMobileCard(
    index: Int,
    item: AnimeViewingOrderItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** В ленте хронологии номер и год показывает нода таймлайна, на постере они дублировали бы её. */
    showIndexAndYear: Boolean = true,
) {
    val shape = MaterialTheme.shapes.medium
    MobilePosterCard(
        title = item.title,
        posterUrls = item.poster.urlsByQuality(LocalPosterQuality.current),
        modifier = modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier),
        caption = item.relation,
        captionMinLines = 2,
        rating = item.rating,
        animeId = item.animeId,
        posterOverlay = {
            item.type?.takeIf { it.isNotBlank() }?.let { MobilePosterType(it) }
            if (showIndexAndYear) {
                ViewingOrderMobileIndex(index)
                item.year?.let { MobilePosterYear(it) }
            }
        },
        onClick = onClick,
    )
}
