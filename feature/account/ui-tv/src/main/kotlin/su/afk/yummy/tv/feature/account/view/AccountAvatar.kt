@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import su.afk.yummy.tv.core.designsystem.seasonal.SeasonalAvatarFrame
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
internal fun AccountAvatar(avatarUrl: String, nickname: String) {
    SeasonalAvatarFrame(avatarSize = 92.dp) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
