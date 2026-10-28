package su.afk.yummy.tv.core.designsystem.mobile.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.TitleListRowContent
import su.afk.yummy.tv.core.designsystem.components.yearText
import su.afk.yummy.tv.core.designsystem.locals.LocalLibraryListStatuses
import su.afk.yummy.tv.core.designsystem.locals.LocalLibraryUserRatings
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi

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
        TitleListRowContent(
            item = item,
            nowEpochSeconds = nowEpochSeconds,
            year = year,
            showViewerMark = showViewerMark,
            trailingAction = trailingAction,
        )
        if (viewerListStatus != null || viewerRating != null) {
            MobileViewerMarkStrip(listStatus = viewerListStatus, rating = viewerRating)
        }
    }
}
