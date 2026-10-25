package su.afk.yummy.tv.core.designsystem.mobile.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
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
import su.afk.yummy.tv.core.designsystem.components.FavoriteMarker
import su.afk.yummy.tv.core.designsystem.components.ListStatusChip
import su.afk.yummy.tv.core.designsystem.components.ReleaseStatusChip
import su.afk.yummy.tv.core.designsystem.components.UserRatingChip
import su.afk.yummy.tv.core.designsystem.components.UserRatingTag
import su.afk.yummy.tv.core.designsystem.components.releaseCountdownText
import su.afk.yummy.tv.core.designsystem.components.yearText
import su.afk.yummy.tv.core.designsystem.locals.LocalLibraryListStatuses
import su.afk.yummy.tv.core.designsystem.locals.LocalLibraryUserRatings
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus

/**
 * Строка тайтла из пользовательского списка (библиотека, списки профиля): постер 2:3 с оценкой
 * сайта и капсулой оценки пользователя, справа — название, год, чипы статуса
 * выхода / любимых / списка, дата и отсчёт до серии. [trailingAction] встаёт в правый верхний угол, дата — в правый нижний.
 *
 * В чужом списке ([showViewerMark]) оценка владельца уходит в ряд чипов, год — в угол постера, а свои список и оценка —
 * в подписанную полосу под карточкой, чтобы не путать, чьё что.
 */
@Composable
fun MobileTitleListCard(
    item: TitleListCardUi,
    nowEpochSeconds: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showYear: Boolean = true,
    /** Полоса «У вас: список ★оценка» под карточкой — для чужих списков. */
    showViewerMark: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val viewerListStatus = if (showViewerMark) LocalLibraryListStatuses.current[item.animeId] else null
    val viewerRating = if (showViewerMark) LocalLibraryUserRatings.current[item.animeId] else null
    val ownerRating = item.userRating?.takeIf { it in 1..10 }
    val year = item.yearText(showYear)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .clickablePointer()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MobileTitleListCardHeight),
        ) {
            Box(
                modifier = Modifier
                    .width(MobileTitleListPosterWidth)
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
                    // Без звезды: звезда — у капсулы оценки пользователя внизу постера.
                    MobileRatingBadge(
                        rating = rating,
                        showStar = false,
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
        if (viewerListStatus != null || viewerRating != null) {
            MobileViewerMarkStrip(listStatus = viewerListStatus, rating = viewerRating)
        }
    }
}

@Composable
private fun TitleListCardChips(item: TitleListCardUi, year: String?, ownerRating: Int?) {
    // «Вышел» — состояние большинства тайтлов, подпись о нём только шумит.
    val releaseStatus = item.releaseStatus?.takeIf { it != AnimeReleaseStatus.RELEASED }
    if (year == null && releaseStatus == null && !item.isFavorite && item.listStatus == null && ownerRating == null) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        year?.let { YearChip(year = it) }
        releaseStatus?.let { ReleaseStatusChip(status = it) }
        ownerRating?.let { UserRatingTag(rating = it) }
        item.listStatus?.let { ListStatusChip(status = it) }
        if (item.isFavorite) FavoriteMarker()
    }
}

/** Год выхода плашкой, чтобы не сливался с фоном карточки, как обычный серый текст. */
@Composable
private fun YearChip(year: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = year,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

private val MobileTitleListCardHeight = 144.dp
private val MobileTitleListPosterWidth = 96.dp
