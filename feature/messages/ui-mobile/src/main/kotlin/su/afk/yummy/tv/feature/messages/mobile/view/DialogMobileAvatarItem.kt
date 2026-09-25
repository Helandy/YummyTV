package su.afk.yummy.tv.feature.messages.mobile.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer
import su.afk.yummy.tv.domain.messages.model.DialogSummary
import su.afk.yummy.tv.feature.messages.mobile.utils.displayName

/** Диалог в свёрнутой панели списка рядом с открытым чатом: только аватарка и счётчик. */
@Composable
internal fun DialogMobileAvatarItem(dialog: DialogSummary, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        BadgedBox(
            badge = {
                if (dialog.unreadCount > 0) {
                    Badge { Text(dialog.unreadCount.coerceAtMost(99).toString()) }
                }
            },
        ) {
            val name = dialog.displayName()
            DialogMobileAvatar(
                avatarUrl = dialog.avatarUrl,
                name = name,
                modifier = Modifier
                    .semantics { contentDescription = name }
                    .clip(CircleShape)
                    .clickablePointer()
                    .clickable(onClick = onClick),
            )
        }
    }
}
