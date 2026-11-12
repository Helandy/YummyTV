package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.rememberTvTextFieldEditing
import su.afk.yummy.tv.core.designsystem.focus.tvTextFieldEditing
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.designsystem.permissions.rememberLocalNetworkAccessRequest
import su.afk.yummy.tv.core.designsystem.utils.addUrlHighlights
import su.afk.yummy.tv.core.model.settings.TorrentSettings
import su.afk.yummy.tv.feature.settings.common.utils.hintText
import su.afk.yummy.tv.feature.settings.common.utils.warningText
import su.afk.yummy.tv.feature.settings.model.TorrServerCheckStatus
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft

/** Секция «Торренты» для ТВ: тумблер, адрес TorrServer и индексатор раздач (JacRed/Jackett). */
@Composable
internal fun TorrentSettingsPanel(
    settings: TorrentSettings,
    checkAvailable: Boolean,
    checkStatus: TorrServerCheckStatus,
    onCheck: () -> Unit,
    upFocusRequester: FocusRequester,
    onChange: (TorrentSettings) -> Unit,
    contentFocusRequester: FocusRequester,
    /** Строка выбора поля сортировки: список вариантов открывается в третьей колонке. */
    sortPickerRow: @Composable (Modifier) -> Unit,
) {
    val leftToTab = Modifier.restoreCategoryFocusOnLeft(upFocusRequester)
    // Local Network Protection (Android 16+): без разрешения сокеты к 192.168.x.x молча виснут до таймаута.
    val requestLocalNetworkAccess = rememberLocalNetworkAccessRequest()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSectionTitle(stringResource(R.string.settings_torrent_title))
        settings.warningText(checkStatus)?.let { warning ->
            Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        ToggleRow(
            label = stringResource(R.string.settings_torrent_enabled_label),
            hint = stringResource(
                if (settings.enabled) R.string.settings_torrent_enabled_on else R.string.settings_torrent_enabled_off,
            ),
            enabled = settings.enabled,
            onClick = {
                if (!settings.enabled) requestLocalNetworkAccess()
                onChange(settings.copy(enabled = !settings.enabled))
            },
            modifier = Modifier.focusRequester(contentFocusRequester).then(leftToTab),
        )
        val hint = stringResource(R.string.settings_torrent_hint)
        val linkStyle = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        )
        Text(
            text = buildAnnotatedString {
                append(hint)
                addUrlHighlights(hint, linkStyle)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        if (!settings.enabled) return@Column

        TorrentTvField(
            value = settings.torrServerUrl,
            label = stringResource(R.string.settings_torrent_server_label),
            placeholder = stringResource(R.string.settings_torrent_server_placeholder),
            keyboardType = KeyboardType.Uri,
            modifier = leftToTab,
            onValueChange = { onChange(settings.copy(torrServerUrl = it.trim())) },
        )
        TorrentTvField(
            value = settings.torrServerUser,
            label = stringResource(R.string.settings_torrent_user_label),
            modifier = leftToTab,
            onValueChange = { onChange(settings.copy(torrServerUser = it.trim())) },
        )
        TorrentTvField(
            value = settings.torrServerPassword,
            label = stringResource(R.string.settings_torrent_password_label),
            keyboardType = KeyboardType.Password,
            isSecret = true,
            modifier = leftToTab,
            onValueChange = { onChange(settings.copy(torrServerPassword = it)) },
        )
        if (checkAvailable) {
            TorrentCheckRow(
                label = stringResource(R.string.settings_torrent_check_label),
                hint = checkStatus.hintText(),
                enabled = settings.torrServerUrl.isNotBlank(),
                onClick = {
                    // Во время проверки строка остаётся фокусируемой (иначе фокус уходит в меню); повторный запуск гасит ViewModel.
                    if (checkStatus != TorrServerCheckStatus.Checking) {
                        requestLocalNetworkAccess()
                        onCheck()
                    }
                },
                modifier = leftToTab,
            )
        }
        ToggleRow(
            label = stringResource(R.string.settings_torrent_jacred_label),
            hint = stringResource(
                if (settings.jacRedEnabled) R.string.settings_torrent_jacred_on else R.string.settings_torrent_jacred_off,
            ),
            enabled = settings.jacRedEnabled,
            onClick = { onChange(settings.copy(jacRedEnabled = !settings.jacRedEnabled)) },
            modifier = leftToTab,
        )
        if (settings.jacRedEnabled) {
            TorrentTvField(
                value = settings.jacRedUrl,
                label = stringResource(R.string.settings_torrent_jacred_url_label),
                placeholder = TorrentSettings.DEFAULT_INDEXER_URL,
                keyboardType = KeyboardType.Uri,
                modifier = leftToTab,
                onValueChange = { onChange(settings.copy(jacRedUrl = it.trim())) },
            )
        }
        ToggleRow(
            label = stringResource(R.string.settings_torrent_jackett_label),
            hint = stringResource(
                if (settings.jackettEnabled) R.string.settings_torrent_jackett_on else R.string.settings_torrent_jackett_off,
            ),
            enabled = settings.jackettEnabled,
            onClick = { onChange(settings.copy(jackettEnabled = !settings.jackettEnabled)) },
            modifier = leftToTab,
        )
        if (settings.jackettEnabled) {
            TorrentTvField(
                value = settings.jackettUrl,
                label = stringResource(R.string.settings_torrent_jackett_url_label),
                placeholder = TorrentSettings.JACKETT_URL_HINT,
                keyboardType = KeyboardType.Uri,
                modifier = leftToTab,
                onValueChange = { onChange(settings.copy(jackettUrl = it.trim())) },
            )
            TorrentTvField(
                value = settings.jackettApiKey,
                label = stringResource(R.string.settings_torrent_jackett_key_label),
                modifier = leftToTab,
                onValueChange = { onChange(settings.copy(jackettApiKey = it.trim())) },
            )
        }
        sortPickerRow(leftToTab)
        ToggleRow(
            label = stringResource(R.string.settings_torrent_sort_reversed_label),
            hint = stringResource(
                if (settings.sortReversed) {
                    R.string.settings_torrent_sort_reversed_on
                } else {
                    R.string.settings_torrent_sort_reversed_off
                },
            ),
            enabled = settings.sortReversed,
            onClick = { onChange(settings.copy(sortReversed = !settings.sortReversed)) },
            modifier = leftToTab,
        )
    }
}

/** Кнопка-строка «Проверить TorrServer» с подписью-итогом под названием; в стиле [ToggleRow], но без переключателя. */
@Composable
private fun TorrentCheckRow(
    label: String,
    hint: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else 0.58f }
            .clip(shape)
            .border(width = 2.dp, color = if (focused) colors.primary else Color.Transparent, shape = shape)
            .background(if (focused) colors.onSurface.copy(alpha = 0.08f) else Color.Transparent, shape)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
        )
        // Строка подписи занимает место всегда: иначе при правке адреса итог проверки пропадает, высота меняется и поля «прыгают».
        Text(
            text = hint.ifEmpty { " " },
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun TorrentTvField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isSecret: Boolean = false,
) {
    val editing = rememberTvTextFieldEditing()
    // Показываем сырой ввод, а наружу уходит обрезанное значение: иначе пробел срезается на лету и курсор прыгает.
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != text.trim()) text = value }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        singleLine = true,
        // Правка и клавиатура включаются по OK: иначе фокус стрелками открывает клавиатуру и экран дрожит.
        readOnly = !editing.isEditing,
        shape = RoundedCornerShape(10.dp),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { editing.stop() }),
        visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier
            .fillMaxWidth()
            // Скролл колонки целится в всё поле, а не в курсор: иначе при смене фокуса/правки она подрагивает на 1 px.
            .tvWholeItemBringIntoView()
            .padding(horizontal = 8.dp)
            .tvTextFieldEditing(editing),
    )
}
