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
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.components.FavoriteMarker
import su.afk.yummy.tv.core.designsystem.components.ListStatusBadge
import su.afk.yummy.tv.core.designsystem.components.ListStatusBadgeHeight
import su.afk.yummy.tv.core.designsystem.components.ListStatusChip
import su.afk.yummy.tv.core.designsystem.components.ReleaseStatusChip
import su.afk.yummy.tv.core.designsystem.components.UserRatingChip
import su.afk.yummy.tv.core.designsystem.components.releaseCountdownText
import su.afk.yummy.tv.core.designsystem.components.yearText
import su.afk.yummy.tv.core.designsystem.locals.LocalLibraryListStatuses
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus

/**
 * Строка тайтла из пользовательского списка (библиотека, списки профиля): постер 2:3 с оценкой
 * сайта и капсулой оценки пользователя, справа — название, год, чипы статуса
 * выхода / любимых / списка, дата и отсчёт до серии. [trailingAction] встаёт в правый нижний угол.
 */
@Composable
fun MobileTitleListCard(
    item: TitleListCardUi,
    nowEpochSeconds: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showYear: Boolean = true,
    /** Плашка «в каком списке тайтл у меня» по нижней кромке постера — для чужих списков. */
    showMyListStatus: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MobileTitleListCardHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .clickablePointer()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .width(MobileTitleListPosterWidth)
                .fillMaxHeight(),
        ) {
            AsyncImage(
                model = item.posterUrl,
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
            val myListStatus = if (showMyListStatus) LocalLibraryListStatuses.current[item.animeId] else null
            // Оценка пользователя — капсулой на постере, как на ТВ и в деталке; над плашкой списка.
            item.userRating?.takeIf { it in 1..10 }?.let { rating ->
                UserRatingChip(
                    rating = rating,
                    textStyle = MaterialTheme.typography.labelLarge,
                    iconSize = 14.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = if (myListStatus != null) ListStatusBadgeHeight else 0.dp)
                        .padding(4.dp),
                )
            }
            myListStatus?.let { status ->
                ListStatusBadge(
                    status = status,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TitleListCardChips(item, year = item.yearText(showYear))
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item.dateText?.takeIf { it.isNotBlank() }?.let { date ->
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    releaseCountdownText(item.nextEpisodeAtSeconds, nowEpochSeconds)?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                trailingAction?.invoke()
            }
        }
    }
}

@Composable
private fun TitleListCardChips(item: TitleListCardUi, year: String?) {
    // «Вышел» — состояние большинства тайтлов, подпись о нём только шумит.
    val releaseStatus = item.releaseStatus?.takeIf { it != AnimeReleaseStatus.RELEASED }
    if (year == null && releaseStatus == null && !item.isFavorite && item.listStatus == null) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        year?.let { YearChip(year = it) }
        releaseStatus?.let { ReleaseStatusChip(status = it) }
        if (item.isFavorite) FavoriteMarker()
        item.listStatus?.let { ListStatusChip(status = it) }
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
