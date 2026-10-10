package su.afk.yummy.tv.feature.home.mobile.view

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
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterCard
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.bestUrl

@Composable
internal fun HomeItemCard(
    item: HomeFeedItem,
    showMetadata: Boolean,
    showYear: Boolean,
    /** Подпись под названием; в блоке новых серий — дата выхода серии. */
    subtitle: String? = null,
    /** Бейдж поверх постера; в блоке новых серий — номер вышедшей серии. */
    badge: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    MobilePosterCard(
        title = item.title,
        posterUrl = item.poster.bestUrl(),
        subtitle = subtitle ?: item.description.takeIf { showMetadata && it.isNotBlank() },
        badge = badge,
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
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
