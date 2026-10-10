package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.focus.TvRetryButton
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.presentation.R

/** Действия над серией по удержанию OK на карточке: пока это только отметка о просмотре. */
@Composable
internal fun EpisodeActionsTvDialog(
    action: EpisodesState.EpisodeAction,
    onToggleWatched: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismissFocusRequester = remember { FocusRequester() }
    // Диалог открывается по удержанию OK, и отпускание кнопки прилетает уже сюда: Compose нажимает
    // кнопку по KeyUp, поэтому «хвост» долгого нажатия съедаем, иначе диалог закроется сам.
    var ignoreLongPressRelease by remember { mutableStateOf(true) }

    // По умолчанию фокус на «Отмена» — отметка не должна срабатывать случайным нажатием OK.
    LaunchedEffect(Unit) {
        runCatching { dismissFocusRequester.requestFocus() }
        // KeyUp долгого нажатия может и не дойти до диалога (пульт отпустили до его появления),
        // поэтому окно перехвата в любом случае закрываем по таймауту.
        delay(LONG_PRESS_RELEASE_WINDOW_MS)
        ignoreLongPressRelease = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .onPreviewKeyEvent { event ->
                    if (!ignoreLongPressRelease) return@onPreviewKeyEvent false
                    if (event.key !in ConfirmKeys) return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyUp) ignoreLongPressRelease = false
                    true
                },
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.details_episode_number,
                        action.episode,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvRetryButton(
                        text = stringResource(CoreR.string.common_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(dismissFocusRequester),
                    )
                    TvRetryButton(
                        text = if (action.isWatched) {
                            stringResource(R.string.details_tv_unmark_episode_watched)
                        } else {
                            stringResource(R.string.details_mark_episode_watched)
                        },
                        onClick = onToggleWatched,
                    )
                }
            }
        }
    }
}

private val ConfirmKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)
private const val LONG_PRESS_RELEASE_WINDOW_MS = 350L
