package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.search.utils.requestFocusOrFalse

@Composable
internal fun YearField(
    label: String,
    value: Int?,
    onValueChanged: (Int?) -> Unit,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { text ->
            onValueChanged(text.filter { it.isDigit() }.take(4).toIntOrNull())
        },
        label = { Text(label) },
        readOnly = !editing,
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties {
                upFocusRequester?.let { up = it }
                downFocusRequester?.let { down = it }
                leftFocusRequester?.let { left = it }
                rightFocusRequester?.let { right = it }
            }
            .onFocusChanged { if (!it.isFocused) editing = false }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        if (editing) false else upFocusRequester.requestFocusOrFalse()
                    }

                    Key.DirectionDown -> {
                        if (editing) false else downFocusRequester.requestFocusOrFalse()
                    }

                    Key.DirectionLeft -> {
                        if (editing) false else leftFocusRequester.requestFocusOrFalse()
                    }

                    Key.DirectionRight -> {
                        if (editing) false else rightFocusRequester.requestFocusOrFalse()
                    }

                    Key.DirectionCenter,
                    Key.Enter,
                    Key.NumPadEnter,
                        -> {
                        if (!editing) {
                            editing = true
                            keyboardController?.show()
                            true
                        } else {
                            false
                        }
                    }

                    Key.Back -> {
                        if (editing) {
                            keyboardController?.hide()
                            editing = false
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            },
    )
}
