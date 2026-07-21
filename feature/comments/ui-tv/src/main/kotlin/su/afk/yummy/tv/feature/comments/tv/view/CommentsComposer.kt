package su.afk.yummy.tv.feature.comments.tv.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.comments.CommentsState
import su.afk.yummy.tv.feature.comments.tv.R

@Composable
internal fun CommentsComposer(
    isSignedIn: Boolean,
    text: String,
    mode: CommentsState.ComposerMode,
    enabled: Boolean,
    isEditing: Boolean,
    focusRequester: FocusRequester,
    onEditingChanged: (Boolean) -> Unit,
    onTextChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(isEditing) {
        if (isEditing) keyboard?.show() else keyboard?.hide()
    }
    if (!isSignedIn) {
        Text(
            text = stringResource(R.string.comments_sign_in_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when (mode) {
                    CommentsState.ComposerMode.New -> stringResource(R.string.comments_new_comment)
                    is CommentsState.ComposerMode.Reply -> stringResource(
                        R.string.comments_reply_to,
                        mode.replyToName,
                    )

                    is CommentsState.ComposerMode.Edit -> stringResource(R.string.comments_editing)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (mode !is CommentsState.ComposerMode.New) {
                TextButton(onClick = onCancel, enabled = enabled) {
                    Text(stringResource(R.string.comments_cancel))
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                enabled = enabled,
                readOnly = !isEditing,
                minLines = 1,
                maxLines = 3,
                placeholder = { Text(stringResource(R.string.comments_input_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onEditingChanged(false)
                    keyboard?.hide()
                }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && isEditing) onEditingChanged(false)
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (!isEditing) {
                                    onEditingChanged(true)
                                    true
                                } else {
                                    false
                                }
                            }

                            Key.Back, Key.Escape -> {
                                if (isEditing) {
                                    onEditingChanged(false)
                                    keyboard?.hide()
                                    true
                                } else {
                                    false
                                }
                            }

                            else -> false
                        }
                    },
            )
            Button(
                onClick = {
                    onEditingChanged(false)
                    keyboard?.hide()
                    onSubmit()
                },
                enabled = enabled && text.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Text(
                    text = stringResource(R.string.comments_send),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
