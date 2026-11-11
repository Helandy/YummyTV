package su.afk.yummy.tv.core.designsystem.dialog

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.tv.TvOverlayAppear
import kotlin.time.Duration.Companion.seconds

/** ТВ-вид выбора из списка: оверлей как у подтверждения, фокус на первой строке (или на «Отмена»). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TvChoiceDialog(
    request: ChoiceDialogRequest,
    onAnswer: (String?) -> Unit,
) {
    val state by request.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val firstFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }
    val items = (state as? ChoiceDialogState.Items)?.items.orEmpty()
    val hasRetry = (state as? ChoiceDialogState.Message)?.retryText != null

    // Окно диалога получает фокус не сразу; при смене содержимого (загрузка → список) фокус ставим заново.
    LaunchedEffect(request, items.isNotEmpty(), hasRetry) {
        focusManager.clearFocus(force = true)
        requestFocusUntilTimeout(
            when {
                items.isNotEmpty() -> firstFocusRequester
                hasRetry -> retryFocusRequester
                else -> cancelFocusRequester
            },
            timeout = 3.seconds,
        )
    }

    Dialog(
        onDismissRequest = { onAnswer(null) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onAnswer(null) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            TvOverlayAppear {
                Column(
                    modifier = Modifier
                        .width(720.dp)
                        .heightIn(max = 560.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(YummySemanticColors.OverlayScrim)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                        .focusGroup()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Back || event.key == Key.Escape)
                            ) {
                                onAnswer(null)
                                true
                            } else {
                                false
                            }
                        }
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = request.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    when (val current = state) {
                        ChoiceDialogState.Loading, is ChoiceDialogState.AutoSelect -> TvChoiceProgress(text = null)

                        is ChoiceDialogState.Progress -> TvChoiceProgress(text = current.text)

                        is ChoiceDialogState.Message -> {
                            Text(
                                text = current.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            current.retryText?.let { retryText ->
                                TvChoiceOption(
                                    title = retryText,
                                    centered = true,
                                    modifier = Modifier.focusRequester(retryFocusRequester),
                                    onClick = { onAnswer(ChoiceDialogCenter.RETRY_ID) },
                                )
                            }
                        }

                        is ChoiceDialogState.Items -> LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(current.items, key = { it.id }) { item ->
                                TvChoiceOption(
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    badges = item.badges,
                                    markers = item.markers,
                                    warnings = item.warnings,
                                    stats = item.stats,
                                    modifier = if (item.id == current.items.first().id) {
                                        Modifier.focusRequester(firstFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                    onClick = { onAnswer(item.id) },
                                )
                            }
                        }
                    }
                    TvChoiceOption(
                        title = request.cancelText,
                        centered = true,
                        modifier = Modifier.focusRequester(cancelFocusRequester),
                        onClick = { onAnswer(null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TvChoiceOption(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    badges: List<String> = emptyList(),
    markers: List<String> = emptyList(),
    warnings: List<String> = emptyList(),
    stats: List<ChoiceDialogStat> = emptyList(),
    centered: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        targetValue = if (focused) Color.White else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(durationMillis = 150),
        label = "choiceOptionBg",
    )
    val titleColor = if (focused) Color.Black else Color.White
    val mutedColor = if (focused) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.70f)
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        ChoiceBadges(
            badges = badges,
            markers = markers,
            warnings = warnings,
            chipColor = if (focused) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.14f),
            onChipColor = if (focused) Color.Black.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.90f),
            accentColor = if (focused) accent else accent.copy(alpha = 0.30f),
            onAccentColor = if (focused) Color.Black else accent,
        )
        ChoiceStats(stats = stats, neutralColor = mutedColor)
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = mutedColor,
            )
        }
    }
}

/** Шаг ожидания: спиннер слева, пояснение справа (если есть). */
@Composable
private fun TvChoiceProgress(text: String?) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = Color.White.copy(alpha = 0.85f),
            strokeWidth = 3.dp,
        )
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
