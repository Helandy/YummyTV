package su.afk.yummy.tv.feature.account

import androidx.navigation3.runtime.NavKey

interface IAccountNavigator {
    fun getAccountDest(): NavKey
    fun getUserProfileDest(userId: Int): NavKey
    fun getUserProfileByNicknameDest(nickname: String): NavKey
    fun getUserSearchDest(): NavKey
    fun getMySubscriptionsDest(): NavKey
    fun getProfileEditDest(): NavKey
    fun getPasswordResetDest(): NavKey

    /** Экран регистрации есть только в мобильной сборке — на ТВ entry не зарегистрирован. */
    fun getRegistrationDest(): NavKey

    /** Передача сессии на ТВ по локальной сети — только мобильная сборка. */
    fun getLocalAuthDest(): NavKey
}
