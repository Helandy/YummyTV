package su.afk.yummy.tv.feature.details.details.utils

import androidx.compose.ui.focus.FocusRequester
import su.afk.yummy.tv.core.model.settings.DetailsSectionKind

/** Ключ фокуса блока ленты деталки; у рядов к нему добавляется «:индекс» карточки. */
internal val DetailsSectionKind.focusKey: String
    get() = name.lowercase()

/** Ряд карточек (вход — запомненная карточка), а не одиночный блок. */
internal val DetailsSectionKind.isFocusRow: Boolean
    get() = when (this) {
        DetailsSectionKind.SCREENSHOTS,
        DetailsSectionKind.VIDEO,
        DetailsSectionKind.UNIVERSE,
        DetailsSectionKind.COLLECTIONS,
        -> true
        else -> false
    }

/** requestFocus без падения, если реквестер сейчас ни к чему не привязан (блок не скомпонован, хвоста описания нет). */
internal fun FocusRequester.tryRequestFocus(): Boolean = runCatching { requestFocus() }.getOrDefault(false)
