package su.afk.yummy.tv.feature.account.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object AccountDestination : NavKey

@Serializable
data class UserProfileDestination(val userId: Int) : NavKey
