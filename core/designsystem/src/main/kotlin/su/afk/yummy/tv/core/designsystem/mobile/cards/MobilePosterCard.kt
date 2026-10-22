package su.afk.yummy.tv.core.designsystem.mobile.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.components.ListStatusBadge
import su.afk.yummy.tv.core.designsystem.components.ListStatusBadgeHeight
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterTextPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterTitleStyle
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterTitleWeight
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterWidth
import su.afk.yummy.tv.core.designsystem.locals.LocalLibraryListStatuses
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer
import su.afk.yummy.tv.core.designsystem.mobile.input.onSecondaryClick
import su.afk.yummy.tv.core.designsystem.seasonal.seasonalCardDecor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobilePosterCard(
    title: String,
    posterUrl: String?,
    modifier: Modifier = Modifier.width(currentMobilePosterWidth()),
    subtitle: String? = null,
    /** Акцентная строка под [subtitle] — например вид связи тайтла («продолжение»). */
    caption: String? = null,
    badge: String? = null,
    rating: Double? = null,
    titleMinLines: Int = 2,
    /** Если задан — карточка подписывается списком пользователя, в котором лежит тайтл. */
    animeId: Int? = null,
    posterOverlay: @Composable BoxScope.() -> Unit = {},
    /** Слот под карточкой — например кнопки голосования. Живёт внутри Card, клик не перехватывает. */
    footer: @Composable ColumnScope.() -> Unit = {},
    /** Долгое нажатие по карточке — например контекстное меню тайтла. */
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            Modifier
                .clickablePointer()
                .onSecondaryClick(onLongClick)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(TITLE_POSTER_ASPECT_RATIO)
                    .seasonalCardDecor(animeId ?: posterUrl.hashCode()),
            ) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentScale = ContentScale.Crop,
                )
                if (!badge.isNullOrBlank()) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                rating?.takeIf { it > 0.0 }?.let { value ->
                    MobileRatingBadge(
                        rating = value,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                    )
                }
                val listStatus = animeId?.let { LocalLibraryListStatuses.current[it] }
                // Плашка занимает нижнюю кромку постера, поэтому нижние оверлеи (год) поднимаем над ней.
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(bottom = if (listStatus != null) ListStatusBadgeHeight else 0.dp),
                ) {
                    posterOverlay()
                }
                listStatus?.let { status ->
                    ListStatusBadge(
                        status = status,
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }
            }
            Column(Modifier.padding(horizontal = currentMobilePosterTextPadding(), vertical = 8.dp)) {
                Text(
                    text = title,
                    minLines = titleMinLines,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = currentMobilePosterTitleStyle(),
                    fontWeight = currentMobilePosterTitleWeight(),
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!caption.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        footer()
    }
}
