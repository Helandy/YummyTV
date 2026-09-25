package su.afk.yummy.tv.feature.messages.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.navigation.scene.CompactListPaneDestination

@Serializable
data object DialogsDestination : NavKey

@Serializable
data class ChatDestination(
    val userId: Int,
    val nickname: String = "",
    val avatarUrl: String? = null,
) : NavKey, CompactListPaneDestination
