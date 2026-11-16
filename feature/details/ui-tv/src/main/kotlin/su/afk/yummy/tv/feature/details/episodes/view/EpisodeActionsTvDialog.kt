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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.focus.TvRetryButton
import su.afk.yummy.tv.core.designsystem.focus.consumeLongPressRelease
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.presentation.R

/** Действия над серией по удержанию OK на карточке: пока это только отметка о просмотре. */
@Composable
internal fun EpisodeActionsTvDialog(
    action: EpisodesState.EpisodeAction,
    onToggleWatched: () -> Unit,
    onClearWatched: () -> Unit,
    onToggleWatchedUpTo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismissFocusRequester = remember { FocusRequester() }
    // По умолчанию фокус на «Отмена» — отметка не должна срабатывать случайным нажатием OK.
    LaunchedEffect(Unit) {
        runCatching { dismissFocusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .consumeLongPressRelease(),
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
                    if (!action.isWatched && action.hasProgress) {
                        TvRetryButton(
                            text = stringResource(R.string.details_tv_unmark_episode_watched),
                            onClick = onClearWatched,
                        )
                    }
                }
                TvRetryButton(
                    text = if (action.isWatched) {
                        stringResource(R.string.details_unmark_episodes_up_to_watched)
                    } else {
                        stringResource(R.string.details_mark_episodes_up_to_watched)
                    },
                    onClick = onToggleWatchedUpTo,
                )
            }
        }
    }
}
