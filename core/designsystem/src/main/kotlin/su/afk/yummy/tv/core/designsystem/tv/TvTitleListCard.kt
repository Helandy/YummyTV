package su.afk.yummy.tv.core.designsystem.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.FavoriteMarker
import su.afk.yummy.tv.core.designsystem.components.ListStatusBadge
import su.afk.yummy.tv.core.designsystem.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.components.ReleaseStatusChip
import su.afk.yummy.tv.core.designsystem.components.UserRatingChip
import su.afk.yummy.tv.core.designsystem.components.releaseCountdownText
import su.afk.yummy.tv.core.designsystem.components.releaseDateText
import su.afk.yummy.tv.core.designsystem.components.yearText
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus

/**
 * Постер тайтла из пользовательского списка для ТВ-гридов: на постере — статус выхода и ♥ слева
 * сверху, оценка сайта справа, внизу капсула оценки пользователя и год, под ними (если
 * задан) список пользователя; под постером — дата и отсчёт до серии.
 */
@Composable
fun TvTitleListCard(
    item: TitleListCardUi,
    nowEpochSeconds: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    width: Dp? = null,
    showYear: Boolean = true,
) {
    val year = item.yearText(showYear)
    val countdown = releaseCountdownText(item.nextEpisodeAtSeconds, nowEpochSeconds)
    val releaseDate = releaseDateText(item.nextEpisodeAtSeconds, nowEpochSeconds)
    TvTitleCard(
        title = item.title,
        posterUrls = item.posterUrls,
        onClick = onClick,
        onFocused = onFocused,
        onLongClick = onLongClick,
        modifier = modifier,
        width = width,
        subtitle = item.dateText?.takeIf { it.isNotBlank() },
        // Две строки: сколько осталось и когда именно выйдет серия.
        caption = listOfNotNull(countdown, releaseDate).joinToString("\n").ifBlank { null },
        posterOverlay = {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // «Вышел» — состояние большинства тайтлов, на постере это шум.
                item.releaseStatus
                    ?.takeIf { it != AnimeReleaseStatus.RELEASED }
                    ?.let { ReleaseStatusChip(status = it) }
                if (item.isFavorite) FavoriteMarker(iconSize = 14.dp)
            }
            item.siteRating?.takeIf { it > 0.0 }?.let { rating ->
                RatingBadge(
                    rating = rating,
                    showStar = false,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item.userRating?.takeIf { it in 1..10 }?.let { rating ->
                        UserRatingChip(
                            rating = rating,
                            textStyle = MaterialTheme.typography.titleSmall,
                            iconSize = 16.dp,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    year?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
                item.listStatus?.let { status -> ListStatusBadge(status = status) }
            }
        },
    )
}
