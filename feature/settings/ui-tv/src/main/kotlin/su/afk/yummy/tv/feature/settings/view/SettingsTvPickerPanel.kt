package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.model.SettingsTvPicker
import su.afk.yummy.tv.feature.settings.utils.availableAppThemes
import su.afk.yummy.tv.feature.settings.utils.color
import su.afk.yummy.tv.feature.settings.utils.hint
import su.afk.yummy.tv.feature.settings.utils.label
import su.afk.yummy.tv.feature.settings.utils.toDetailsButtonOrderItems

/** Текущее значение пункта-пикера для строки в центральной колонке. */
@Composable
internal fun SettingsTvPicker.valueText(state: SettingsState.State): String = when (this) {
    SettingsTvPicker.INTERFACE_MODE -> state.interfaceMode.label()
    SettingsTvPicker.CONTENT_LANGUAGE -> state.contentLanguage.label()
    SettingsTvPicker.THEME -> state.appTheme.label()
    SettingsTvPicker.BACKGROUND -> state.backgroundStyle.label()
    SettingsTvPicker.POSTER_SIZE -> state.posterCardSize.label()
    SettingsTvPicker.CONTINUE_WATCHING_SIZE -> state.libraryContinueWatchingCardSize.label()
    SettingsTvPicker.POSTER_QUALITY -> state.posterQuality.label()
    SettingsTvPicker.DETAILS_BUTTON_ORDER ->
        state.detailsButtonOrder.toDetailsButtonOrderItems().joinToString(" · ") { it.label }

    SettingsTvPicker.PREFERRED_PLAYER -> state.preferredPlayer.label()
    SettingsTvPicker.SUBTITLE_COLOR -> state.subtitleStyle.textColor.label()
    SettingsTvPicker.SUBTITLE_BACKGROUND -> state.subtitleStyle.background.label()
}

/**
 * Третья колонка ТВ-настроек: варианты открытого пункта.
 * Фокус входа — на выбранном варианте ([entryFocusRequester]); «влево» закрывает колонку ([onClose]),
 * выбор варианта применяет его и тоже закрывает колонку.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SettingsTvPickerPanel(
    picker: SettingsTvPicker,
    state: SettingsState.State,
    entryFocusRequester: FocusRequester,
    openerFocusRequester: FocusRequester,
    onInterfaceModeSelected: (AppInterfaceMode) -> Unit,
    onEvent: (SettingsState.Event) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .focusProperties {
                onExit = {
                    if (requestedFocusDirection == FocusDirection.Left) {
                        onClose()
                    }
                }
            }
            .focusGroup()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsSectionTitle(text = stringResource(picker.titleRes))
        val select: (SettingsState.Event) -> Unit = {
            onEvent(it)
            onClose()
        }
        when (picker) {
            SettingsTvPicker.INTERFACE_MODE -> PickerOptions(
                values = AppInterfaceMode.entries,
                selected = state.interfaceMode,
                labelOf = { it.label() },
                hintOf = { it.hint() },
                focusRequester = entryFocusRequester,
                onSelected = {
                    onClose()
                    onInterfaceModeSelected(it)
                },
            )

            SettingsTvPicker.CONTENT_LANGUAGE -> PickerOptions(
                values = YaniContentLanguage.entries,
                selected = state.contentLanguage,
                labelOf = { it.label() },
                focusRequester = entryFocusRequester,
                onSelected = { select(SettingsState.Event.ContentLanguageSelected(it)) },
            )

            SettingsTvPicker.THEME -> PickerOptions(
                values = availableAppThemes,
                selected = state.appTheme,
                labelOf = { it.label() },
                hintOf = { it.hint() },
                focusRequester = entryFocusRequester,
                onSelected = { select(SettingsState.Event.AppThemeSelected(it)) },
            )

            SettingsTvPicker.BACKGROUND -> PickerOptions(
                values = BackgroundStyle.entries,
                selected = state.backgroundStyle,
                labelOf = { it.label() },
                hintOf = { it.hint() },
                focusRequester = entryFocusRequester,
                onSelected = { select(SettingsState.Event.BackgroundStyleSelected(it)) },
            )

            SettingsTvPicker.POSTER_SIZE -> PickerOptions(
                values = PosterCardSize.entries,
                selected = state.posterCardSize,
                labelOf = { it.label() },
                hintOf = { it.hint() },
                focusRequester = entryFocusRequester,
                onSelected = { select(SettingsState.Event.PosterCardSizeSelected(it)) },
            )

            SettingsTvPicker.CONTINUE_WATCHING_SIZE -> PickerOptions(
                values = LibraryContinueWatchingCardSize.entries,
                selected = state.libraryContinueWatchingCardSize,
                labelOf = { it.label() },
                hintOf = { it.hint() },
                focusRequester = entryFocusRequester,
                onSelected = {
                    select(SettingsState.Event.LibraryContinueWatchingCardSizeSelected(it))
                },
            )

            SettingsTvPicker.POSTER_QUALITY -> PickerOptions(
                values = PosterQuality.entries,
                selected = state.posterQuality,
                labelOf = { it.label() },
                hintOf = { it.hint() },
                focusRequester = entryFocusRequester,
                onSelected = { select(SettingsState.Event.PosterQualitySelected(it)) },
            )

            // Порядок кнопок правится по шагам, поэтому колонка остаётся открытой.
            SettingsTvPicker.DETAILS_BUTTON_ORDER -> DetailsButtonOrderPanel(
                order = state.detailsButtonOrder,
                upFocusRequester = openerFocusRequester,
                contentFocusRequester = entryFocusRequester,
                onMoveUp = {
                    onEvent(SettingsState.Event.DetailsButtonMoved(it, DetailsButtonMoveDirection.UP))
                },
                onMoveDown = {
                    onEvent(SettingsState.Event.DetailsButtonMoved(it, DetailsButtonMoveDirection.DOWN))
                },
                onReset = { onEvent(SettingsState.Event.DetailsButtonOrderReset) },
            )

            SettingsTvPicker.PREFERRED_PLAYER -> PickerOptions(
                values = PreferredPlayer.entries,
                selected = state.preferredPlayer,
                labelOf = { it.label() },
                hintOf = { it.hint() },
                focusRequester = entryFocusRequester,
                onSelected = { select(SettingsState.Event.PreferredPlayerSelected(it)) },
            )

            SettingsTvPicker.SUBTITLE_COLOR -> PickerOptions(
                values = PlayerSubtitleTextColor.entries,
                selected = state.subtitleStyle.textColor,
                labelOf = { it.label() },
                colorOf = { it.color },
                focusRequester = entryFocusRequester,
                onSelected = {
                    select(
                        SettingsState.Event.SubtitleStyleSelected(state.subtitleStyle.copy(textColor = it)),
                    )
                },
            )

            SettingsTvPicker.SUBTITLE_BACKGROUND -> PickerOptions(
                values = PlayerSubtitleBackground.entries,
                selected = state.subtitleStyle.background,
                labelOf = { it.label() },
                focusRequester = entryFocusRequester,
                onSelected = {
                    select(
                        SettingsState.Event.SubtitleStyleSelected(state.subtitleStyle.copy(background = it)),
                    )
                },
            )
        }
    }
}

/** Радио-список вариантов; [focusRequester] вешается на выбранный вариант (или первый, если выбранного нет). */
@Composable
private fun <T> PickerOptions(
    values: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    focusRequester: FocusRequester,
    onSelected: (T) -> Unit,
    hintOf: (@Composable (T) -> String)? = null,
    colorOf: (@Composable (T) -> Color)? = null,
) {
    val focusIndex = values.indexOf(selected).coerceAtLeast(0)
    values.forEachIndexed { index, value ->
        QualityRow(
            label = labelOf(value),
            hint = hintOf?.invoke(value).orEmpty(),
            selected = value == selected,
            onClick = { onSelected(value) },
            labelColor = colorOf?.invoke(value),
            modifier = if (index == focusIndex) Modifier.focusRequester(focusRequester) else Modifier,
        )
        if (index < values.lastIndex) {
            SettingsDivider()
        }
    }
}
