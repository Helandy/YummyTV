package su.afk.yummy.tv.feature.account.userprofile.handler

import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.comments.CommentTargetType
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.usecase.GetCommentsUseCase
import su.afk.yummy.tv.feature.account.userprofile.mapper.toUserProfileCommentPreview
import su.afk.yummy.tv.feature.account.userprofile.model.UserProfileCommentPreview
import javax.inject.Inject

/** Загружает несколько свежих комментариев пользователя под тайтлами (вместе с ответами) для превью в обзоре. */
internal class UserProfileCommentsHandler @Inject constructor(
    private val getComments: GetCommentsUseCase,
) {
    suspend fun loadPreview(userId: Int): Result<List<UserProfileCommentPreview>> =
        runSuspendCatching {
            getComments(
                targetType = CommentTargetType.USER,
                targetId = userId,
                limit = COMMENTS_PREVIEW_FETCH_LIMIT,
                skip = 0,
                sort = CommentSort.NEW,
            ).comments
                .filter { it.deletedAtEpochSeconds == null && it.text.isNotBlank() }
                .take(COMMENTS_PREVIEW_SIZE)
                .map { it.toUserProfileCommentPreview() }
                .filter { it.text.isNotBlank() }
        }
}

private const val COMMENTS_PREVIEW_SIZE = 3

// С запасом: удалённые отфильтровываются после загрузки.
private const val COMMENTS_PREVIEW_FETCH_LIMIT = 10
