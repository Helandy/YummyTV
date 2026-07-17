package su.afk.yummy.tv.feature.comments.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class CommentsDestination(
    val targetType: String,
    val targetId: Int,
) : NavKey
