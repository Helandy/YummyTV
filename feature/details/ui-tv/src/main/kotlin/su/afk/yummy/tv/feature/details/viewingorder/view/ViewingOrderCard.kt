package su.afk.yummy.tv.feature.details.viewingorder.view

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.tv.TvPosterRating
import su.afk.yummy.tv.core.designsystem.tv.TvPosterType
import su.afk.yummy.tv.core.designsystem.tv.TvPosterYear
import su.afk.yummy.tv.core.designsystem.tv.TvTitleCard
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.core.model.anime.urlsByQuality
import su.afk.yummy.tv.feature.details.view.common.RelatedTitleCardWidth
import su.afk.yummy.tv.feature.details.view.common.TvPosterIndex

@Composable
internal fun ViewingOrderCard(
    index: Int,
    item: AnimeViewingOrderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    /** Тайтл, чья страница сейчас открыта: обводка отличается от фокусной и скрывается, пока карточка в фокусе. */
    selected: Boolean = false,
    /** В ленте хронологии номер и год показывает нода таймлайна, на постере они дублировали бы её. */
    showIndexAndYear: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }

    TvTitleCard(
        title = item.title,
        posterUrls = item.poster.urlsByQuality(LocalPosterQuality.current),
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { focused = it.hasFocus }
            .then(
                if (selected && !focused) {
                    Modifier.border(SelectedBorderWidth, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
                } else {
                    Modifier
                },
            ),
        caption = item.relation,
        captionMinLines = 2,
        onFocused = onFocused,
        width = RelatedTitleCardWidth,
        animeId = item.animeId,
        posterOverlay = {
            item.type?.takeIf { it.isNotBlank() }?.let { TvPosterType(it) }
            if (showIndexAndYear) TvPosterIndex(index)
            item.rating?.let { TvPosterRating(it) }
            if (showIndexAndYear) item.year?.let { TvPosterYear(it) }
        },
    )
}

private val SelectedBorderWidth = 2.dp
