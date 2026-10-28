package su.afk.yummy.tv.feature.library.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi
import su.afk.yummy.tv.core.designsystem.tv.TvTitleListCard

/** Карточка библиотеки ТВ: действия (удалить и т.п.) открываются долгим нажатием. */
@Composable
internal fun LibraryAnimeCard(
    item: TitleListCardUi,
    nowEpochSeconds: Long,
    showYear: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = currentTvTitleCardDimensions().width,
) {
    TvTitleListCard(
        item = item,
        nowEpochSeconds = nowEpochSeconds,
        showYear = showYear,
        onClick = onClick,
        onLongClick = onLongClick,
        onFocused = onFocused,
        modifier = modifier,
        width = cardWidth,
    )
}
