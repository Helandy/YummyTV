package su.afk.yummy.tv.core.designsystem.mobile.cards

import su.afk.yummy.tv.core.designsystem.seasonal.seasonalCardDecor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.components.ListStatusBadge
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import su.afk.yummy.tv.core.designsystem.locals.LocalLibraryListStatuses
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer
import su.afk.yummy.tv.core.designsystem.mobile.input.onSecondaryClick

private val PosterWidth = 96.dp
private const val DESCRIPTION_MAX_LINES = 4

/**
 * Построчная карточка тайтла: постер слева, справа название, год, рейтинг и описание.
 * Высота равна высоте постера; длинное описание обрезается многоточием.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobileTitleRowCard(
    title: String,
    posterUrl: String?,
    modifier: Modifier = Modifier,
    description: String = "",
    year: Int? = null,
    rating: Double? = null,
    /** Если задан — постер подписывается списком пользователя, в котором лежит тайтл. */
    animeId: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(PosterWidth / TITLE_POSTER_ASPECT_RATIO)
                .clickablePointer()
                .onSecondaryClick(onLongClick)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            Box(Modifier.width(PosterWidth).fillMaxHeight().seasonalCardDecor(animeId ?: posterUrl.hashCode())) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentScale = ContentScale.Crop,
                )
                animeId?.let { LocalLibraryListStatuses.current[it] }?.let { status ->
                    ListStatusBadge(status = status, modifier = Modifier.align(Alignment.BottomStart))
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (year != null || rating != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rating?.takeIf { it > 0.0 }?.let { MobileRatingBadge(rating = it) }
                        year?.let {
                            Text(
                                text = it.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description.trim(),
                        maxLines = DESCRIPTION_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
