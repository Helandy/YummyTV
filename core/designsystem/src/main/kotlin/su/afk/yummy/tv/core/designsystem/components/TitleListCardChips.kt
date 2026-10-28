package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus

/**
 * Ряд чипов карточки тайтла из списка: год, статус выхода, оценка владельца, список, «любимое».
 * Общий для мобильной строки и ТВ-строки.
 */
@Composable
fun TitleListCardChips(
    item: TitleListCardUi,
    year: String?,
    ownerRating: Int?,
    modifier: Modifier = Modifier,
) {
    // «Вышел» — состояние большинства тайтлов, подпись о нём только шумит.
    val releaseStatus = item.releaseStatus?.takeIf { it != AnimeReleaseStatus.RELEASED }
    if (year == null && releaseStatus == null && !item.isFavorite && item.listStatus == null && ownerRating == null) return
    Row(
        modifier = modifier,
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
