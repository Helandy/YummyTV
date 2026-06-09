package su.afk.yummy.tv.feature.main.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TvMainContentPane(
    showMainMenu: Boolean,
    contentFocusRequester: FocusRequester,
    selectedRootFocusRequester: FocusRequester,
    currentPreferredContentFocusRequester: FocusRequester?,
    onFocusChanged: (isFocused: Boolean, hasFocus: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = if (showMainMenu) TvSideMenuCollapsedWidth else 0.dp)
            .focusRequester(contentFocusRequester)
            .focusProperties {
                left = selectedRootFocusRequester
                onEnter = {
                    currentPreferredContentFocusRequester?.requestFocus()
                }
            }
            .onFocusChanged {
                onFocusChanged(it.isFocused, it.hasFocus)
            }
            .focusGroup(),
    ) {
        content()
    }
}
