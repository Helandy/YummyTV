package su.afk.yummy.tv.feature.library.view

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.FallbackAsyncImage
import su.afk.yummy.tv.core.designsystem.components.FavoriteMarker
import su.afk.yummy.tv.core.designsystem.components.ListStatusBadge
import su.afk.yummy.tv.core.designsystem.components.MarqueeTitleText
import su.afk.yummy.tv.core.designsystem.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.components.ReleaseStatusChip
import su.afk.yummy.tv.core.designsystem.components.UserRatingChip
import su.afk.yummy.tv.core.designsystem.components.releaseCountdownText
import su.afk.yummy.tv.core.designsystem.components.releaseDateText
import su.afk.yummy.tv.core.designsystem.components.yearText
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi

/**
 * Широкая карточка библиотеки ТВ: постер слева, справа название, статус выхода, оценки, год и —
 * главное для сериала — когда выйдет следующая серия. Действия (удалить и т.п.) открываются долгим
 * нажатием, поэтому под карточкой никаких кнопок нет.
 */
@Composable
internal fun LibraryAnimeCard(
    item: TitleListCardUi,
    nowEpochSeconds: Long,
    showYear: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    cardHeight: Dp = LibraryCardHeight,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    val countdown = releaseCountdownText(item.nextEpisodeAtSeconds, nowEpochSeconds)
    val releaseDate = releaseDateText(item.nextEpisodeAtSeconds, nowEpochSeconds)
    val year = item.yearText(showYear)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .onFocusChanged { state ->
                val focused = state.isFocused || state.hasFocus
                if (focused && !isFocused) onFocused()
                isFocused = focused
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .tvFocusableClick(onClick = onClick, onLongClick = onLongClick, shape = shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(TITLE_POSTER_ASPECT_RATIO)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            FallbackAsyncImage(
                models = item.posterUrls,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.isFavorite) {
                FavoriteMarker(
                    iconSize = 14.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                )
            }
            item.listStatus?.let { status ->
                ListStatusBadge(status = status, modifier = Modifier.align(Alignment.BottomStart))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MarqueeTitleText(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                minLines = 1,
                maxLines = 2,
                isFocused = isFocused,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item.releaseStatus?.let { ReleaseStatusChip(status = it) }
                year?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item.siteRating?.takeIf { it > 0.0 }?.let { RatingBadge(rating = it) }
                item.userRating?.takeIf { it in 1..10 }?.let { rating ->
                    UserRatingChip(
                        rating = rating,
                        textStyle = MaterialTheme.typography.labelLarge,
                        iconSize = 16.dp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (countdown != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    releaseDate?.let { date ->
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            } else {
                item.dateText?.takeIf { it.isNotBlank() }?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Высота широкой карточки; ширина постера выводится из неё по пропорции постера. */
internal val LibraryCardHeight = 188.dp

/** Желаемая ширина широкой карточки: от неё считается число колонок сетки. */
internal val LibraryCardPreferredWidth = 520.dp
