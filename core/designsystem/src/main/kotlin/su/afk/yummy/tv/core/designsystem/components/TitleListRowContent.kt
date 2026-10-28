package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterYear
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileRatingBadge
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi

/**
 * Тело строки тайтла из пользовательского списка — общее для мобильной `MobileTitleListCard` и
 * ТВ-строки: постер с оценками слева, название, чипы, отсчёт до серии и дата справа. Размеры и
 * стили одни на обе платформы, чтобы карточки не расходились.
 *
 * [year] — уже отформатированный год (с учётом настройки показа), [showViewerMark] — чужой список.
 */
@Composable
fun TitleListRowContent(
    item: TitleListCardUi,
    nowEpochSeconds: Long,
    year: String?,
    showViewerMark: Boolean,
    trailingAction: (@Composable () -> Unit)?,
) {
    val ownerRating = item.userRating?.takeIf { it in 1..10 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TitleListRowHeight),
    ) {
        Box(
            modifier = Modifier
                .width(TitleListPosterWidth)
                .fillMaxHeight(),
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
                MobileRatingBadge(
                    rating = rating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                )
            }
            // Своя оценка — капсулой на постере, как на ТВ и в деталке. Чужая — в ряду чипов.
            ownerRating?.takeIf { !showViewerMark }?.let { rating ->
                UserRatingChip(
                    rating = rating,
                    textStyle = MaterialTheme.typography.labelLarge,
                    iconSize = 14.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                )
            }
            // В чужом списке низ постера свободен: год уходит туда, как в каталоге, чтобы разгрузить ряд чипов.
            // В своём слева капсула оценки, рядом на 96dp год не помещается.
            item.year?.takeIf { showViewerMark && year != null }?.let { MobilePosterYear(it) }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                trailingAction?.invoke()
            }
            TitleListCardChips(
                item = item,
                year = year?.takeIf { !showViewerMark },
                ownerRating = ownerRating?.takeIf { showViewerMark },
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val countdown = releaseCountdownText(item.nextEpisodeAtSeconds, nowEpochSeconds)
                Text(
                    text = countdown.orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.dateText?.takeIf { it.isNotBlank() }?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val TitleListRowHeight = 144.dp
private val TitleListPosterWidth = 96.dp
