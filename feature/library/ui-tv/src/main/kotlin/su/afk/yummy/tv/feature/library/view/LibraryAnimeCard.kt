package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions

@Composable
internal fun LibraryAnimeCard(
    title: String,
    posterUrl: String?,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    deleteModifier: Modifier = Modifier,
    cardWidth: Dp = currentTvTitleCardDimensions().width,
    subtitle: String? = null,
    posterOverlay: @Composable (BoxScope.() -> Unit)? = null,
) {
    var isCardFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .onFocusChanged { isCardFocused = it.hasFocus }
            .then(modifier)
            .width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TvTitleCard(
            title = title,
            posterUrl = posterUrl,
            subtitle = subtitle,
            onClick = onClick,
            onFocused = onFocused,
            modifier = cardModifier,
            width = cardWidth,
            posterOverlay = posterOverlay,
        )
        LibraryDeleteButton(
            onClick = onDelete,
            modifier = deleteModifier
                .width(cardWidth)
                .alpha(if (isCardFocused) 1f else 0f),
        )
    }
}
