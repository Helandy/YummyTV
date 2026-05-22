package su.afk.yummy.tv.core.update.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class UpdateDestination(
    val version: String,
    val apkUrl: String,
    val changelog: String,
) : NavKey
