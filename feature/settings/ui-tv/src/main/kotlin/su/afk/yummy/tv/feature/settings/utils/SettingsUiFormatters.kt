package su.afk.yummy.tv.feature.settings.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.theme.isDynamicColorSupported
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.BrowserUserAgentProfile
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.common.utils.hint

@Composable
internal fun LibraryContinueWatchingCardSize.hint(): String = stringResource(
    when (this) {
        LibraryContinueWatchingCardSize.COMPACT ->
            R.string.settings_library_continue_watching_card_size_compact_hint_tv

        LibraryContinueWatchingCardSize.STANDARD ->
            R.string.settings_library_continue_watching_card_size_standard_hint

        LibraryContinueWatchingCardSize.LARGE ->
            R.string.settings_library_continue_watching_card_size_large_hint_tv
    },
)

/**
 * Возврат фокуса из панели контента к списку категорий слева (D-pad влево).
 * Навешивается на первый пункт панели; между строками контента вверх/вниз работает штатно.
 */
internal fun Modifier.restoreCategoryFocusOnLeft(
    focusRequester: FocusRequester,
    enabled: Boolean = true,
    /** Единственная строка в категории: вверх/вниз тоже некуда — возвращаем в список категорий. */
    alsoOnVertical: Boolean = false,
): Modifier = if (enabled) {
    focusProperties {
        left = focusRequester
        if (alsoOnVertical) {
            up = focusRequester
            down = focusRequester
        }
    }
} else {
    this
}
