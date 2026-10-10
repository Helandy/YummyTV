package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
internal fun ProfileMediaSection(
    avatarUrl: String?,
    bannerUrl: String?,
    enabled: Boolean,
    onPick: (ProfileImageKind) -> Unit,
    onDelete: (ProfileImageKind) -> Unit,
) {
    AccountMobileSurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.profile_edit_images),
                style = MaterialTheme.typography.titleMedium
            )
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            ProfileImageActions(
                kind = ProfileImageKind.AVATAR,
                hasImage = !avatarUrl.isNullOrBlank(),
                enabled = enabled,
                onPick = onPick,
                onDelete = onDelete,
                centered = true,
            )
            AsyncImage(
                model = bannerUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            ProfileImageActions(
                kind = ProfileImageKind.BANNER,
                hasImage = !bannerUrl.isNullOrBlank(),
                enabled = enabled,
                onPick = onPick,
                onDelete = onDelete,
                centered = true,
            )
        }
    }
}

@Composable
private fun ProfileImageActions(
    kind: ProfileImageKind,
    hasImage: Boolean,
    enabled: Boolean,
    onPick: (ProfileImageKind) -> Unit,
    onDelete: (ProfileImageKind) -> Unit,
    centered: Boolean = false,
) {
    Row(
        modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
        horizontalArrangement = Arrangement.spacedBy(
            8.dp,
            if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ),
    ) {
        Button(onClick = { onPick(kind) }, enabled = enabled) {
            Text(
                stringResource(
                    if (kind == ProfileImageKind.AVATAR) R.string.profile_edit_choose_avatar
                    else R.string.profile_edit_choose_banner
                )
            )
        }
        if (hasImage) {
            OutlinedButton(onClick = { onDelete(kind) }, enabled = enabled) {
                Text(stringResource(R.string.account_delete))
            }
        }
    }
}
