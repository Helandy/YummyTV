package su.afk.yummy.tv.feature.account.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.account.IAccountNavigator
import javax.inject.Inject

class AccountNavigator @Inject constructor() : IAccountNavigator {
    override fun getAccountDest(): NavKey = AccountDestination
}
