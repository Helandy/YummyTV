package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.model.ReleaseNotesStatus

/** Шаг прокрутки списка изменений одним нажатием DPAD. */
private const val DPAD_SCROLL_STEP_PX = 120

/** Стандартная ширина AlertDialog (до 560dp) мала для списка изменений на ТВ. */
private const val DIALOG_WIDTH_FRACTION = 0.72f
private val DIALOG_MAX_WIDTH = 960.dp

/**
 * Диалог «Что нового» на ТВ. Список изменений прокручивается DPAD, пока в нём фокус; упёршись в
 * край, нажатие отдаётся обычному поиску фокуса — так из начала списка можно подняться к «Закрыть»
 * в шапке. «Повторить» внизу появляется только при ошибке, когда списка нет.
 */
@Composable
internal fun ReleaseNotesTvDialog(
    status: ReleaseNotesStatus,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val listFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    val hasItems = status is ReleaseNotesStatus.Loaded && status.items.isNotEmpty()
    LaunchedEffect(hasItems) {
        runCatching { if (hasItems) listFocusRequester.requestFocus() else closeFocusRequester.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(DIALOG_WIDTH_FRACTION)
            .widthIn(max = DIALOG_MAX_WIDTH),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_tv_release_notes_title),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(closeFocusRequester),
                ) {
                    Text(stringResource(R.string.settings_tv_release_notes_close))
                }
            }
        },
        text = {
            when (status) {
                ReleaseNotesStatus.Idle, ReleaseNotesStatus.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                ReleaseNotesStatus.Error -> ReleaseNotesTvMessage(
                    stringResource(R.string.settings_tv_release_notes_error),
                )

                is ReleaseNotesStatus.Loaded -> if (status.items.isEmpty()) {
                    ReleaseNotesTvMessage(stringResource(R.string.settings_tv_release_notes_empty))
                } else {
                    ReleaseNotesTvList(status = status, focusRequester = listFocusRequester)
                }
            }
        },
        confirmButton = {
            if (status == ReleaseNotesStatus.Error) {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.settings_tv_release_notes_retry))
                }
            }
        },
    )
}

@Composable
private fun ReleaseNotesTvList(
    status: ReleaseNotesStatus.Loaded,
    focusRequester: FocusRequester,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .border(
                width = 2.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val delta = when (event.key) {
                    Key.DirectionDown -> DPAD_SCROLL_STEP_PX
                    Key.DirectionUp -> -DPAD_SCROLL_STEP_PX
                    else -> return@onPreviewKeyEvent false
                }
                val target = (scrollState.value + delta).coerceIn(0, scrollState.maxValue)
                if (target == scrollState.value) return@onPreviewKeyEvent false
                scope.launch { scrollState.animateScrollTo(target) }
                true
            }
            .focusable()
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        status.items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
            ReleaseNoteTvItem(item)
        }
    }
}

@Composable
private fun ReleaseNotesTvMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
