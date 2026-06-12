package su.afk.yummy.tv.feature.details.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun LibraryListPickerOverlay(
    onConfirmed: (UserAnimeList) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val options = remember {
        listOf(
            UserAnimeList.WATCHING,
            UserAnimeList.PLANNED,
            UserAnimeList.COMPLETED,
            UserAnimeList.POSTPONED,
            UserAnimeList.DROPPED,
        )
    }
    val firstFocusRequester = remember { FocusRequester() }
    var focusedOptionIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        withFrameNanos { }
        runCatching { firstFocusRequester.requestFocus() }
        withFrameNanos { }
        runCatching { firstFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .width(440.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF21B1B1F))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                    .focusGroup()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Back, Key.Escape -> {
                                onDismiss()
                                true
                            }

                            Key.DirectionUp -> focusedOptionIndex == 0
                            Key.DirectionDown -> focusedOptionIndex == options.lastIndex
                            else -> false
                        }
                    }
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.details_library_picker_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
                options.forEachIndexed { index, list ->
                    LibraryListOptionItem(
                        label = list.label(),
                        focusRequester = if (index == 0) firstFocusRequester else null,
                        onFocused = { focusedOptionIndex = index },
                        onClick = { onConfirmed(list) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryListOptionItem(
    label: String,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val bgColor = if (focused) Color.White else Color.White.copy(alpha = 0.10f)
    val textColor = if (focused) Color.Black else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.details_library_picker_add),
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.64f),
            maxLines = 1,
        )
    }
}

@Composable
private fun UserAnimeList.label(): String = stringResource(
    when (this) {
        UserAnimeList.WATCHING -> R.string.details_library_list_watching
        UserAnimeList.PLANNED -> R.string.details_library_list_planned
        UserAnimeList.COMPLETED -> R.string.details_library_list_completed
        UserAnimeList.POSTPONED -> R.string.details_library_list_postponed
        UserAnimeList.DROPPED -> R.string.details_library_list_dropped
    }
)
