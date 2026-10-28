package su.afk.yummy.tv.feature.library.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi
import su.afk.yummy.tv.core.designsystem.tv.TvTitleListRow

/** Карточка библиотеки ТВ — строка как на мобилке; действия открываются долгим нажатием. */
@Composable
internal fun LibraryAnimeCard(
    item: TitleListCardUi,
    nowEpochSeconds: Long,
    showYear: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    deleteButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    TvTitleListRow(
        item = item,
        nowEpochSeconds = nowEpochSeconds,
        onClick = onClick,
        onLongClick = onLongClick,
        showYear = showYear,
        trailingAction = deleteButton,
        modifier = modifier.onFocusChanged { state ->
            val focused = state.isFocused || state.hasFocus
            if (focused && !isFocused) onFocused()
            isFocused = focused
        },
    )
}

/** Желаемая ширина строки: от неё считается число колонок сетки. */
internal val LibraryCardPreferredWidth = 440.dp
