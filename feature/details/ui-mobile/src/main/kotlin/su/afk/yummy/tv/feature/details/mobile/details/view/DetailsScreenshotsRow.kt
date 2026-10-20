package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.screenshots.utils.screenshotLazyKey

@Composable
internal fun DetailsScreenshotsRow(
    screenshots: List<AnimeScreenshot>,
    onScreenshotClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (screenshots.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MobileSectionHeader(
            title = stringResource(R.string.details_screenshots_title),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("details_screenshots_row"),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(
                items = screenshots,
                key = { index, screenshot -> screenshot.screenshotLazyKey(index) },
            ) { index, screenshot ->
                Card(
                    onClick = { onScreenshotClick(index) },
                    modifier = Modifier
                        .width(240.dp)
                        .aspectRatio(16f / 9f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    AsyncImage(
                        model = screenshot.small ?: screenshot.full,
                        contentDescription = screenshot.episode?.let {
                            stringResource(R.string.details_episode_number, it)
                        } ?: stringResource(R.string.details_mobile_screenshot),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
