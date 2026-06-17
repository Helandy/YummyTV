package su.afk.yummy.tv.core.designsystem.presenter.focus

import androidx.compose.foundation.focusGroup
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer

fun Modifier.tvFocusRestorer(
    fallback: FocusRequester = FocusRequester.Default,
    enabled: Boolean = true,
): Modifier =
    if (enabled) {
        focusRestorer(fallback).focusGroup()
    } else {
        focusGroup()
    }
