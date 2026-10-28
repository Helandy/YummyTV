package su.afk.yummy.tv.core.designsystem.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import su.afk.yummy.tv.core.designsystem.components.FallbackAsyncImage
import su.afk.yummy.tv.core.designsystem.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.components.TitleListCardChips
import su.afk.yummy.tv.core.designsystem.components.UserRatingChip
import su.afk.yummy.tv.core.designsystem.components.releaseCountdownText
import su.afk.yummy.tv.core.designsystem.components.yearText
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi

/**
 * Строка тайтла из пользовательского списка для ТВ — та же раскладка, что у мобильной
 * `MobileTitleListCard`: постер слева с оценкой сайта и капсулой оценки пользователя, справа
 * название, чипы (год / статус выхода / список / любимое), внизу отсчёт до серии и дата.
 * Размеры крупнее под экран с расстояния; фокус и долгое нажатие — через [tvFocusableClick].
 */
@Composable
fun TvTitleListRow(
    item: TitleListCardUi,
    nowEpochSeconds: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    showYear: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    val year = item.yearText(showYear)
    val userRating = item.userRating?.takeIf { it in 1..10 }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TvTitleListRowHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .tvFocusableClick(onClick = onClick, onLongClick = onLongClick, shape = shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(TITLE_POSTER_ASPECT_RATIO),
        ) {
            FallbackAsyncImage(
                models = item.posterUrls,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            item.siteRating?.takeIf { it > 0.0 }?.let { rating ->
                // Без звезды: звезда — у капсулы оценки пользователя внизу постера.
                RatingBadge(
                    rating = rating,
                    showStar = false,
                    scale = 1.3f,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
            userRating?.let { rating ->
                UserRatingChip(
                    rating = rating,
                    textStyle = MaterialTheme.typography.titleSmall,
                    iconSize = 16.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TitleListCardChips(item = item, year = year, ownerRating = null, scale = 1.25f)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = releaseCountdownText(item.nextEpisodeAtSeconds, nowEpochSeconds).orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.dateText?.takeIf { it.isNotBlank() }?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val TvTitleListRowHeight = 180.dp
