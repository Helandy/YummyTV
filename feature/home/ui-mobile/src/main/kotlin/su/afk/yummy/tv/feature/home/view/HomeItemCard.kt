package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.feature.home.utils.bestUrl

@Composable
internal fun HomeItemCard(
    item: HomeFeedItem,
    showMetadata: Boolean,
    showYear: Boolean,
    onClick: () -> Unit,
) {
    MobilePosterCard(
        title = item.title,
        posterUrl = item.poster.bestUrl(),
        subtitle = item.description.takeIf { showMetadata && it.isNotBlank() },
        rating = item.rating.takeIf { showMetadata },
        titleMinLines = if (showMetadata) 1 else 2,
        posterOverlay = {
            if (showYear) {
                item.year?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.inverseSurface,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.inverseOnSurface,
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        },
        onClick = onClick,
    )
}
