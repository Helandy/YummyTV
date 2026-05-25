package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvFocusOverlay
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.domain.anime.model.AnimeScreenshot
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun ScreenshotCard(
    screenshot: AnimeScreenshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Card(
        modifier = modifier
            .width(284.dp)
            .height(160.dp)
            .tvFocusableClick(onClick = onClick, interactionSource = interactionSource, shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.DarkGray.copy(alpha = 0.55f),
        ),
    ) {
        Box {
            AsyncImage(
                model = screenshot.small ?: screenshot.full,
                contentDescription = screenshot.episode?.let {
                    stringResource(R.string.details_episode_content_description, it)
                },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            TvFocusOverlay(focused = focused, modifier = Modifier.matchParentSize())
        }
    }
}
