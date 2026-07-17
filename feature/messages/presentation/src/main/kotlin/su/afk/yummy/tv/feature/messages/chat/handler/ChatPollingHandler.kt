package su.afk.yummy.tv.feature.messages.chat.handler

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import su.afk.yummy.tv.domain.messages.usecase.GetMessagesUseCase
import javax.inject.Inject

private const val POLLING_INTERVAL_MS = 5_000L
private const val LATEST_MESSAGES_LIMIT = 30

class ChatPollingHandler @Inject constructor(
    private val getMessages: GetMessagesUseCase,
) {
    fun updates(userId: Int) = flow {
        while (currentCoroutineContext().isActive) {
            delay(POLLING_INTERVAL_MS)
            runCatching { getMessages(userId, LATEST_MESSAGES_LIMIT) }
                .getOrNull()
                ?.let { emit(it) }
        }
    }
}
