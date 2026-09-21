package su.afk.yummy.tv.feature.update.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Экран предложения обновиться; [required] — версия больше не поддерживается. */
@Serializable
data class UpdateDestination(
    val version: String,
    val apkUrl: String,
    val changelog: String,
    val required: Boolean = false,
    val updatesCount: Int = 0,
    val isPrerelease: Boolean = false,
) : NavKey
