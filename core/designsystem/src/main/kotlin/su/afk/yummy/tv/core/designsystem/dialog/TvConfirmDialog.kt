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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.time.Duration.Companion.seconds
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.tv.TvOverlayAppear

/** ТВ-вид запроса подтверждения: оверлей как у выбора озвучки, фокус сразу на подтверждающей строке. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TvConfirmDialog(
    request: ConfirmDialogRequest,
    onAnswer: (Boolean) -> Unit,
    /** Закрытие без выбора (Back, тап по фону); по умолчанию это отказ. */
    onDismiss: () -> Unit = { onAnswer(false) },
) {
    val focusManager = LocalFocusManager.current
    val confirmFocusRequester = remember { FocusRequester() }
    val dismissFocusRequester = remember { FocusRequester() }

    // Окно диалога получает фокус не сразу: ждём дольше стандартных 500 мс, иначе фокус остаётся нигде.
    LaunchedEffect(request) {
        focusManager.clearFocus(force = true)
        requestFocusUntilTimeout(confirmFocusRequester, timeout = 3.seconds)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                // диалог — отдельное окно: теги как resource-id для сценариев профиля включаем в нём заново
                .semantics { testTagsAsResourceId = true }
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
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
                            if (event.key == Key.Back || event.key == Key.Escape) {
                                onDismiss()
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    Text(
                        text = request.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    TvConfirmOption(
                        text = request.confirmText,
                        accent = true,
                        focusRequester = confirmFocusRequester,
                        testTag = "confirm_dialog_accept",
                        onClick = { onAnswer(true) },
                    )
                    TvConfirmOption(
                        text = request.dismissText,
                        accent = false,
                        focusRequester = dismissFocusRequester,
                        testTag = "confirm_dialog_dismiss",
                        onClick = { onAnswer(false) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TvConfirmOption(
    text: String,
    accent: Boolean,
    focusRequester: FocusRequester,
    testTag: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        targetValue = if (focused) Color.White else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(durationMillis = 150),
        label = "confirmOptionBg",
    )
    val textColor = when {
        focused -> Color.Black
        accent -> MaterialTheme.colorScheme.primary
        else -> Color.White
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .focusRequester(focusRequester)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
    )
}
