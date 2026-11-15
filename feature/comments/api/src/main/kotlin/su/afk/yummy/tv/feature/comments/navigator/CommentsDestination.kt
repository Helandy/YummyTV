package su.afk.yummy.tv.feature.comments.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class CommentsDestination(
    val targetType: String,
    val targetId: Int,
    val totalCount: Int? = null,
    /** Показать кнопку «Детали» (экран открыт из уведомления, а не с экрана тайтла или поста). */
    val showDetailsAction: Boolean = false,
) : NavKey
