package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.screenshots.utils.screenshotLazyKey
import su.afk.yummy.tv.feature.details.screenshots.view.ScreenshotCard

@Composable
internal fun DetailsScreenshotsRow(
    screenshots: List<AnimeScreenshot>,
    onScreenshotClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemFocusModifier: @Composable (index: Int) -> Modifier = { Modifier },
) {
    if (screenshots.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailsSectionHeader(
            title = stringResource(R.string.details_screenshots_title),
            modifier = Modifier.padding(horizontal = DetailsSectionHorizontalPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = DetailsSectionHorizontalPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = screenshots,
                key = { index, screenshot -> screenshot.screenshotLazyKey(index) },
            ) { index, screenshot ->
                ScreenshotCard(
                    screenshot = screenshot,
                    onClick = { onScreenshotClick(index) },
                    modifier = itemFocusModifier(index),
                )
            }
        }
    }
}
