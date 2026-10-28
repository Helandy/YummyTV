package su.afk.yummy.tv.feature.library.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.tv.TvOverlayAppear
import su.afk.yummy.tv.feature.library.model.LibraryTvChoice

/**
 * Оверлей выбора из списка для библиотеки ТВ: сортировка и действия карточки.
 *
 * Фокус стартует на [initialIndex]. Оверлей часто открывается долгим нажатием, поэтому клики в
 * первые [IgnoreClicksAfterOpenMs] игнорируются: «отпускание» OK не должно выбрать пункт.
 */
@Composable
internal fun LibraryTvChoiceOverlay(
    title: String,
    choices: List<LibraryTvChoice>,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    initialIndex: Int = 0,
) {
    val focusManager = LocalFocusManager.current
    val requesters = remember(choices.size) { List(choices.size) { FocusRequester() } }
    var focusedIndex by remember { mutableIntStateOf(initialIndex) }
    var clicksEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        requesters.getOrNull(initialIndex)?.let { requestFocusUntilTimeout(it) }
        delay(IgnoreClicksAfterOpenMs)
        clicksEnabled = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center,
        ) {
            TvOverlayAppear {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .width(440.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(YummySemanticColors.OverlayScrim)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                        .focusGroup()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.Back, Key.Escape -> {
                                    onDismiss()
                                    true
                                }

                                Key.DirectionUp -> focusedIndex == 0
                                Key.DirectionDown -> focusedIndex == choices.lastIndex
                                else -> false
                            }
                        }
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.70f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    choices.forEachIndexed { index, choice ->
                        LibraryTvChoiceItem(
                            choice = choice,
                            focusRequester = requesters[index],
                            onFocused = { focusedIndex = index },
                            onClick = { if (clicksEnabled) onSelected(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryTvChoiceItem(
    choice: LibraryTvChoice,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val bgColor by animateColorAsState(
        targetValue = if (focused) Color.White else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(durationMillis = 150),
        label = "libraryChoiceBg",
    )
    val baseTextColor = when {
        focused -> Color.Black
        choice.destructive -> YummySemanticColors.Destructive
        else -> Color.White
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
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
        choice.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = baseTextColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = choice.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = baseTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (choice.selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = baseTextColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private const val IgnoreClicksAfterOpenMs = 350L
