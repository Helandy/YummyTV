package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal enum class TvPlayerPanel {
    Quality,
    Dubbing,
    Balancer,
    Speed,
    Resize,
}

internal class TvPlayerPanelsState {
    var activePanel by mutableStateOf<TvPlayerPanel?>(null)
        private set

    var pendingReturnFocusTarget by mutableStateOf<PanelReturnFocusTarget?>(null)

    val isAnyOpen: Boolean
        get() = activePanel != null

    fun isOpen(panel: TvPlayerPanel): Boolean = activePanel == panel

    fun toggle(panel: TvPlayerPanel): Boolean {
        activePanel = panel.takeUnless { it == activePanel }
        return activePanel == panel
    }

    fun close(returnFocusTarget: PanelReturnFocusTarget? = null) {
        if (returnFocusTarget != null) pendingReturnFocusTarget = returnFocusTarget
        activePanel = null
    }
}

@Composable
internal fun rememberTvPlayerPanelsState(): TvPlayerPanelsState =
    remember { TvPlayerPanelsState() }
