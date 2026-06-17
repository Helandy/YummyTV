package su.afk.yummy.tv.feature.main.view

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TvMainContentPane(
    showMainMenu: Boolean,
    menuExpanded: Boolean,
    contentFocusRequester: FocusRequester,
    selectedRootFocusRequester: FocusRequester,
    currentPreferredContentFocusRequester: FocusRequester?,
    onFocusChanged: (isFocused: Boolean, hasFocus: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val slideOffset by animateDpAsState(
        targetValue = if (showMainMenu && menuExpanded) {
            TvSideMenuExpandedWidth - TvSideMenuCollapsedWidth
        } else {
            0.dp
        },
        animationSpec = tween(durationMillis = TvSideMenuAnimationDurationMillis),
        label = "TvMainContentPaneOffset",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = if (showMainMenu) TvSideMenuCollapsedWidth else 0.dp)
            .offset(x = slideOffset)
            .focusRequester(contentFocusRequester)
            .focusProperties {
                left = selectedRootFocusRequester
            }
            .tvFocusRestorer(
                fallback = currentPreferredContentFocusRequester ?: FocusRequester.Default,
            )
            .onFocusChanged {
                onFocusChanged(it.isFocused, it.hasFocus)
            }
    ) {
        content()
    }
}
