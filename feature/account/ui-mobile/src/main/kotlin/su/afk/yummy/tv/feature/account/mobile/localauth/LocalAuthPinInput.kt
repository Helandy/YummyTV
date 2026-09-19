package su.afk.yummy.tv.feature.account.mobile.localauth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.LocalAuthCode
import su.afk.yummy.tv.feature.account.localauth.LocalAuthState

/**
 * Ввод кода с ТВ: ячейки с акцентом темы вместо обычного поля.
 *
 * Само поле невидимо — оно нужно только ради системной клавиатуры и каретки, а рисуем мы
 * ячейки в `decorationBox`. Разбивка по [LocalAuthCode.GROUP_SIZE] повторяет то, как код показан
 * на ТВ, а ячейки узкие: десять штук должны поместиться в ширину телефона.
 */
@Composable
internal fun LocalAuthPinInput(
    pin: String,
    enabled: Boolean,
    isError: Boolean,
    onPinChange: (String) -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    BasicTextField(
        // Каретку всегда держим в конце: пользователь вводит код слева направо.
        value = TextFieldValue(text = pin, selection = TextRange(pin.length)),
        onValueChange = { value ->
            val code = LocalAuthCode.normalize(value.text).take(LocalAuthState.PIN_LENGTH)
            if (code != pin) {
                onPinChange(code)
                if (code.length == LocalAuthState.PIN_LENGTH) onCompleted()
            }
        },
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            // Код теперь буквенно-цифровой, но без подсказок и автозамены: это не слово.
            keyboardType = KeyboardType.Ascii,
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onCompleted() }),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = {
            // Поле растянуто на всю ширину ради тач-зоны, сами ячейки держим по центру.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(LocalAuthState.PIN_LENGTH) { index ->
                        if (index != 0 && index % LocalAuthCode.GROUP_SIZE == 0) {
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        PinCell(
                            digit = pin.getOrNull(index),
                            isActive = enabled && index == pin.length,
                            isError = isError,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun PinCell(
    digit: Char?,
    isActive: Boolean,
    isError: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    val targetBorder = when {
        isError -> colorScheme.error
        isActive || digit != null -> colorScheme.primary
        else -> colorScheme.outlineVariant
    }
    val borderColor by animateColorAsState(targetBorder, label = "pinCellBorder")
    val borderWidth by animateDpAsState(if (isActive) 2.dp else 1.dp, label = "pinCellBorderWidth")

    Box(
        modifier = Modifier
            .size(width = 26.dp, height = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
            )
        } else {
            // Пустая ячейка: короткое тире вместо цифры, чтобы блок не «скакал» по высоте.
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colorScheme.outlineVariant),
            )
        }
    }
}
