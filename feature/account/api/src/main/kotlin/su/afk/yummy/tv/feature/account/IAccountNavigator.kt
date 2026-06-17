package su.afk.yummy.tv.feature.account

import androidx.navigation3.runtime.NavKey

interface IAccountNavigator {
    fun getAccountDest(): NavKey
    fun getUserProfileDest(userId: Int): NavKey
}
