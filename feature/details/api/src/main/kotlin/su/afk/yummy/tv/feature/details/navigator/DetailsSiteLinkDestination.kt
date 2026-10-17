package su.afk.yummy.tv.feature.details.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.navigation.scene.FullscreenDestination

/**
 * Промежуточный экран для ссылки сайта `/catalog/item/{slug}`: по slug узнаёт id тайтла и
 * заменяет себя деталями.
 */
@Serializable
data class DetailsSiteLinkDestination(val slug: String) : NavKey, FullscreenDestination
