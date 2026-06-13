package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.search.R
import su.afk.yummy.tv.feature.search.utils.requestFocusOrFalse

@Composable
internal fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        content()
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }
}

@Composable
internal fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

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

@Composable
internal fun SelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val borderColor = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> Color.Transparent
    }
    val backgroundColor = when {
        selected && focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        focused -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .clip(shape)
            .border(width = 2.dp, color = borderColor, shape = shape)
            .background(backgroundColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = if (selected) stringResource(R.string.search_filter_selected, label) else label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
    }
}
