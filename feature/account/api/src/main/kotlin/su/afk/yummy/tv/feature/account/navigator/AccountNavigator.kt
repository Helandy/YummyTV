package su.afk.yummy.tv.feature.account.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.account.IAccountNavigator

class AccountNavigator : IAccountNavigator {
    override fun getAccountDest(): NavKey = AccountDestination
    override fun getUserProfileDest(userId: Int): NavKey = UserProfileDestination(userId)
    override fun getUserProfileByNicknameDest(nickname: String): NavKey =
        UserProfileByNicknameDestination(nickname)

    override fun getUserSearchDest(): NavKey = UserSearchDestination
    override fun getProfileEditDest(): NavKey = ProfileEditDestination
    override fun getPasswordResetDest(): NavKey = PasswordResetDestination
}
