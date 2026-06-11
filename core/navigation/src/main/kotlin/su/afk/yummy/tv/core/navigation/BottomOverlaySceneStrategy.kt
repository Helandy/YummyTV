package su.afk.yummy.tv.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

fun bottomOverlay(): Map<String, Any> = metadata {
    put(BottomOverlayKey, true)
}

class BottomOverlaySceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val overlayEntry = entries.lastOrNull() ?: return null
        val overlaidEntries = entries.dropLast(1)
        if (overlayEntry.metadata[BottomOverlayKey] != true || overlaidEntries.isEmpty()) {
            return null
        }
        return BottomOverlayScene(
            key = overlayEntry.contentKey,
            entry = overlayEntry,
            previousEntries = overlaidEntries,
            overlaidEntries = overlaidEntries,
        )
    }
}

private object BottomOverlayKey : NavMetadataKey<Boolean>

private class BottomOverlayScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            entry.Content()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BottomOverlayScene<*>

        return key == other.key &&
                entry == other.entry &&
                previousEntries == other.previousEntries &&
                overlaidEntries == other.overlaidEntries
    }

    override fun hashCode(): Int {
        return key.hashCode() * 31 +
                entry.hashCode() * 31 +
                previousEntries.hashCode() * 31 +
                overlaidEntries.hashCode() * 31
    }
}
