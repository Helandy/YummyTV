package su.afk.yummy.tv.feature.messages

import androidx.navigation3.runtime.NavKey

interface IMessagesNavigator {
    fun dialogs(): NavKey
    fun chat(userId: Int, nickname: String = "", avatarUrl: String? = null): NavKey
}
