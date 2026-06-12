package su.afk.yummy.tv.feature.details.screenshots.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.focus.focusRestorerContainer
import su.afk.yummy.tv.core.designsystem.presenter.focus.focusRestorerItem
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberFocusRestorerState
import su.afk.yummy.tv.domain.anime.model.AnimeScreenshot
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun ScreenshotsRow(
    screenshots: List<AnimeScreenshot>,
    onScreenshotClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val restorerState = rememberFocusRestorerState()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.details_frames),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = Modifier.focusRestorerContainer(restorerState),
        ) {
            itemsIndexed(
                items = screenshots,
                key = { _, s -> s.id ?: s.hashCode() }) { index, screenshot ->
                ScreenshotCard(
                    screenshot = screenshot,
                    onClick = { onScreenshotClick(index) },
                    modifier = Modifier.focusRestorerItem(index, restorerState),
                )
            }
        }
    }
}
