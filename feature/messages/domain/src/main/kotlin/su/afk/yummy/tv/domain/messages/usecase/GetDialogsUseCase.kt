package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

/** Загружает страницу диалогов, при необходимости поднимая диалог с указанным пользователем. */
class GetDialogsUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(limit: Int, offset: Int, needUserId: Int? = null) =
        repository.dialogs(limit, offset, needUserId)
}
