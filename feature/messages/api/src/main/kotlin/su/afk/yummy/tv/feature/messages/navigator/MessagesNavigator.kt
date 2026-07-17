package su.afk.yummy.tv.feature.messages.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.messages.IMessagesNavigator

class MessagesNavigator : IMessagesNavigator {
    override fun dialogs(): NavKey = DialogsDestination
    override fun chat(userId: Int, nickname: String, avatarUrl: String?): NavKey =
        ChatDestination(userId, nickname, avatarUrl)
}
