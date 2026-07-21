package su.afk.yummy.tv.feature.player.mobile.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import su.afk.yummy.tv.feature.player.common.PlayerAutoHideController
import su.afk.yummy.tv.feature.player.common.rememberPlayerAutoHideController

/** Видимость оверлея плеера с авто-скрытием через [PlayerAutoHideController]. */
@Stable
internal class MobilePlayerOverlayController(
    visibleState: MutableState<Boolean>,
    private val autoHide: PlayerAutoHideController,
    private val wantsPlay: () -> Boolean,
    private val isPromptVisible: () -> Boolean,
) {
    var visible: Boolean by visibleState

    fun scheduleHide() = autoHide.schedule()

    fun cancelHide() = autoHide.cancel()

    fun show() {
        visible = true
        if (wantsPlay()) autoHide.schedule() else autoHide.cancel()
    }

    fun toggle() {
        if (isPromptVisible()) {
            return
        }
        if (visible) {
            autoHide.cancel()
            visible = false
        } else {
            show()
        }
    }
}

@Composable
internal fun rememberMobilePlayerOverlayController(
    canHide: () -> Boolean,
    wantsPlay: () -> Boolean,
    isPromptVisible: () -> Boolean,
): MobilePlayerOverlayController {
    val visibleState = remember { mutableStateOf(true) }
    val autoHide = rememberPlayerAutoHideController(
        canHide = canHide,
        onHide = { visibleState.value = false },
    )
    val currentWantsPlay = rememberUpdatedState(wantsPlay)
    val currentIsPromptVisible = rememberUpdatedState(isPromptVisible)
    return remember {
        MobilePlayerOverlayController(
            visibleState = visibleState,
            autoHide = autoHide,
            wantsPlay = { currentWantsPlay.value.invoke() },
            isPromptVisible = { currentIsPromptVisible.value.invoke() },
        )
    }
}
