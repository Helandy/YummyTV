package su.afk.yummy.tv.feature.account.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.account.IAccountNavigator

class AccountNavigator : IAccountNavigator {
    override fun getAccountDest(): NavKey = AccountDestination
}
