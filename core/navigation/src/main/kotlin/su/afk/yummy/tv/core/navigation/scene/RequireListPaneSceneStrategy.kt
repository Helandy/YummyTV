package su.afk.yummy.tv.core.navigation.scene

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * Добавить к metadata списка вместе с `ListDetailSceneStrategy.listPane(...)`: по этой метке
 * [RequireListPaneSceneStrategy] понимает, что список действительно лежит в сцене.
 */
fun listPaneAnchor(): Map<String, Any> = metadata {
    put(ListPaneAnchorKey, true)
}

/**
 * Пропускает двухпанельную сцену [delegate] только если в ней есть экран списка.
 * Material list-detail стратегия строит сцену и из одной детали — например, рецензии,
 * открытой со страницы аниме, — и рисует слева пустую панель вместо полноэкранной детали.
 */
class RequireListPaneSceneStrategy<T : Any>(
    private val delegate: SceneStrategy<T>,
) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val scope = this
        val scene = with(delegate) { scope.calculateScene(entries) } ?: return null
        return scene.takeIf { it.entries.any { entry -> entry.metadata[ListPaneAnchorKey] == true } }
    }
}

private object ListPaneAnchorKey : NavMetadataKey<Boolean>
