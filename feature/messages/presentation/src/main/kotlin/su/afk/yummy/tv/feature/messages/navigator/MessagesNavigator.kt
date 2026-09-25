package su.afk.yummy.tv.feature.messages.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.messages.IMessagesNavigator
import javax.inject.Inject

class MessagesNavigator @Inject constructor() : IMessagesNavigator {
    override fun dialogs(): NavKey = DialogsDestination
    override fun chat(userId: Int, nickname: String, avatarUrl: String?): NavKey =
        ChatDestination(userId, nickname, avatarUrl)
}
