package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Синхронный UI-флаг выхода, блокирующий новые TV-промпты до удаления плеера со стека. */
@Stable
internal class TvPlayerExitState {
    var requested: Boolean by mutableStateOf(false)
        private set

    fun request() {
        requested = true
    }
}
