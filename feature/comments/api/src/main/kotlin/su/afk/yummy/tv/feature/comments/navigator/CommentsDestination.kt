package su.afk.yummy.tv.feature.comments.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class AnimeCommentsDestination(val animeId: Int) : NavKey
