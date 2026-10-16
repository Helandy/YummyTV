package su.afk.yummy.tv.feature.catalog.utils

import androidx.compose.ui.focus.FocusRequester

internal fun FocusRequester?.requestFocusOrFalse(): Boolean =
    this != null && runCatching { requestFocus() }.getOrDefault(false)
