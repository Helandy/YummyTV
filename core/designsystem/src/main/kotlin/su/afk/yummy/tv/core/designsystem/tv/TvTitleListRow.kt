package su.afk.yummy.tv.core.designsystem.tv

import androidx.compose.foundation.background
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
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi

/**
 * Строка тайтла из пользовательского списка для ТВ. Содержимое — общий [TitleListRowContent], тот
 * же, что у мобильной `MobileTitleListCard`; здесь только ТВ-фокус и долгое нажатие.
 */
@Composable
fun TvTitleListRow(
    item: TitleListCardUi,
    nowEpochSeconds: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    showYear: Boolean = true,
    /** Действие в правом верхнем углу (крестик удаления), как `trailingAction` на мобилке. */
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Фокус (масштаб и рамка) — снаружи clip: иначе скруглённый clip срезает и то и другое.
            .tvFocusableClick(onClick = onClick, onLongClick = onLongClick, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)),
    ) {
        TitleListRowContent(
            item = item,
            nowEpochSeconds = nowEpochSeconds,
            year = item.yearText(showYear),
            showViewerMark = false,
            trailingAction = trailingAction,
        )
    }
}
