@file:JvmName("MobileHomeSupportPromptDialogKt")

package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.GithubStarIcon
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.feature.home.presentation.R

@Composable
internal fun HomeSupportPromptDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val repositoryUrl = stringResource(R.string.home_support_prompt_url)
    MobileAlertDialog(
        onDismissRequest = onDismiss,
        icon = { GithubStarIcon(Modifier.size(56.dp)) },
        title = {
            Text(
                text = stringResource(R.string.home_support_prompt_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        confirmText = stringResource(R.string.home_support_prompt_ok),
        onConfirm = {
            context.openExternalUri(repositoryUrl)
            onDismiss()
        },
        content = { Text(stringResource(R.string.home_support_prompt_message)) },
    )
}
