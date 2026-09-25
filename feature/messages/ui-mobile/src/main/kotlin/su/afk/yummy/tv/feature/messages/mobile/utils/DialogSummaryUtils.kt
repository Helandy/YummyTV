package su.afk.yummy.tv.feature.messages.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.messages.model.DialogSummary
import su.afk.yummy.tv.feature.messages.mobile.R

/** Имя собеседника для списка диалогов: ник, а без него — общий чат или «пользователь N». */
@Composable
internal fun DialogSummary.displayName(): String = nickname.ifBlank {
    if (userId == 0) {
        stringResource(R.string.messages_global_chat)
    } else {
        stringResource(R.string.messages_unknown_user, userId)
    }
}

/** Буква для аватарки-заглушки: первая буква или цифра имени, без кавычек и эмодзи. */
internal fun String.avatarInitial(): String =
    firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
