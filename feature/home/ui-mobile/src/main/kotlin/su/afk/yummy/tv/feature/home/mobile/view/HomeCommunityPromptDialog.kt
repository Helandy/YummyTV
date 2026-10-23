@file:JvmName("MobileHomeCommunityPromptDialogKt")

package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import su.afk.yummy.tv.core.designsystem.components.AppBrandIcon
import su.afk.yummy.tv.feature.home.presentation.R

/** Приглашение вступить в Telegram-сообщество проекта. */
@Composable
internal fun HomeCommunityPromptDialog(
    onLater: () -> Unit,
    onJoin: () -> Unit,
) {
    Dialog(onDismissRequest = onLater) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppBrandIcon(modifier = Modifier.size(96.dp))
                Text(
                    text = stringResource(R.string.home_community_prompt_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.home_community_prompt_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_community_prompt_join))
                    }
                    OutlinedButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_community_prompt_later))
                    }
                }
            }
        }
    }
}
