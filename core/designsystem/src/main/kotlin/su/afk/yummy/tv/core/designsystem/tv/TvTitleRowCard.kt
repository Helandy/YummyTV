package su.afk.yummy.tv.core.designsystem.tv

import su.afk.yummy.tv.core.designsystem.seasonal.seasonalCardDecor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.components.ListStatusBadge
import su.afk.yummy.tv.core.designsystem.components.MarqueeTitleText
import su.afk.yummy.tv.core.designsystem.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.locals.LocalLibraryListStatuses

private val PosterWidth = 110.dp
private const val DESCRIPTION_MAX_LINES = 3

/**
 * Построчная ТВ-карточка тайтла: постер слева, справа название, год, рейтинг и описание.
 * Растягивается на всю ширину ячейки грида; фокус и [onFocused] — как у [TvTitleCard].
 */
@Composable
fun TvTitleRowCard(
    title: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "",
    year: Int? = null,
    rating: Double? = null,
    onFocused: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    /** Если задан — постер подписывается списком пользователя, в котором лежит тайтл. */
    animeId: Int? = null,
) {
    val shape = MaterialTheme.shapes.medium
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                val focused = focusState.isFocused || focusState.hasFocus
                if (focused && !isFocused) onFocused()
                isFocused = focused
            }
            .tvFocusableClick(onClick = onClick, shape = shape, onLongClick = onLongClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.fillMaxWidth().height(PosterWidth / TITLE_POSTER_ASPECT_RATIO)) {
            Box(
                modifier = Modifier
                    .width(PosterWidth)
                    .fillMaxHeight()
                    .seasonalCardDecor(animeId ?: posterUrl.hashCode())
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (posterUrl == null) {
                    Text(
                        text = title.take(1),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                    )
                }
                animeId?.let { LocalLibraryListStatuses.current[it] }?.let { status ->
                    ListStatusBadge(status = status, modifier = Modifier.align(Alignment.BottomStart))
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MarqueeTitleText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    minLines = 1,
                    maxLines = 1,
                    isFocused = isFocused,
                )
                if (year != null || rating != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rating?.takeIf { it > 0.0 }?.let { RatingBadge(rating = it) }
                        year?.let {
                            Text(
                                text = it.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = DESCRIPTION_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
