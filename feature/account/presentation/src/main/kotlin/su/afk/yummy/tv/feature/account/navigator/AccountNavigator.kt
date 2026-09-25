package su.afk.yummy.tv.feature.account.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.account.IAccountNavigator
import javax.inject.Inject

class AccountNavigator @Inject constructor() : IAccountNavigator {
    override fun getAccountDest(): NavKey = AccountDestination
    override fun getUserProfileDest(userId: Int): NavKey = UserProfileDestination(userId)
    override fun getUserProfileByNicknameDest(nickname: String): NavKey =
        UserProfileByNicknameDestination(nickname)

    override fun getUserSearchDest(): NavKey = UserSearchDestination
    override fun getMySubscriptionsDest(): NavKey = MySubscriptionsDestination
    override fun getProfileEditDest(): NavKey = ProfileEditDestination
    override fun getPasswordResetDest(): NavKey = PasswordResetDestination
    override fun getRegistrationDest(): NavKey = RegistrationDestination
    override fun getLocalAuthDest(): NavKey = LocalAuthDestination
}
