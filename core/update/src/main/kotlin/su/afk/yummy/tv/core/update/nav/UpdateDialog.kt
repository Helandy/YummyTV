package su.afk.yummy.tv.core.update.nav

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.update.R
import su.afk.yummy.tv.core.update.UpdateState
import su.afk.yummy.tv.core.utils.openExternalUri

@Composable
fun UpdateDialog(
    status: UpdateState.State.Status,
    onEvent: (UpdateState.Event) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isTelevision = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
    val maxDialogWidth = if (isTelevision) 760.dp else 680.dp
    val horizontalFill = if (isTelevision) 0.72f else 0.92f
    val dialogPadding = if (configuration.screenWidthDp < 420) 20.dp else 32.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(horizontalFill)
                .widthIn(max = maxDialogWidth)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(dialogPadding),
        ) {
            when (status) {
                is UpdateState.State.Status.Available -> AvailableContent(status, onEvent)
                is UpdateState.State.Status.Downloading -> DownloadingContent(status)
                is UpdateState.State.Status.Installing -> InstallingContent()
                is UpdateState.State.Status.Error -> ErrorContent(status, onEvent)
                else -> {}
            }
        }
    }
}

@Composable
private fun AvailableContent(
    status: UpdateState.State.Status.Available,
    onEvent: (UpdateState.Event) -> Unit,
) {
    val autoUpdateSupported = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.O }
    val configuration = LocalConfiguration.current
    val isTelevision = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
    val changelogMinHeight = if (configuration.screenHeightDp < 640) 160.dp else 200.dp
    val changelogMaxHeight = when {
        configuration.screenHeightDp < 640 -> 220.dp
        isTelevision -> 320.dp
        else -> 280.dp
    }
    val closeFocus = remember { FocusRequester() }
    val updateFocus = remember { FocusRequester() }
    val changelogScrollState = rememberScrollState()
    val changelogFocus = remember { FocusRequester() }
    val formattedChangelog = remember(status.changelog) {
        status.changelog.formatReleaseNotes()
    }
    val updateInteractionSource = remember { MutableInteractionSource() }
    val updateFocused by updateInteractionSource.collectIsFocusedAsState()
    val scope = rememberCoroutineScope()
    var changelogFocused by remember { mutableStateOf(false) }
    LaunchedEffect(autoUpdateSupported) {
        runCatching {
            if (autoUpdateSupported) {
                updateFocus.requestFocus()
            } else {
                closeFocus.requestFocus()
            }
        }
    }

    Column {
        Text(
            text = stringResource(
                if (status.required) R.string.update_required_title else R.string.update_available_title
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.update_version, status.version),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        if (status.required) {
            Spacer(Modifier.height(16.dp))
            RequiredUpdateBanner()
        }

        if (!autoUpdateSupported) {
            Spacer(Modifier.height(16.dp))
            UnsupportedAutoUpdateBanner()
        }

        if (formattedChangelog.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.update_changelog_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = changelogMinHeight, max = changelogMaxHeight)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = if (changelogFocused) 2.dp else 1.dp,
                        color = if (changelogFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(8.dp),
                    )
                    .focusRequester(changelogFocus)
                    .onFocusChanged { changelogFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        val delta = when (event.key) {
                            Key.DirectionDown -> 72
                            Key.DirectionUp -> -72
                            else -> return@onPreviewKeyEvent false
                        }
                        val target = (changelogScrollState.value + delta)
                            .coerceIn(0, changelogScrollState.maxValue)
                        if (target == changelogScrollState.value) return@onPreviewKeyEvent false

                        scope.launch { changelogScrollState.animateScrollTo(target) }
                        true
                    }
                    .focusable()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = formattedChangelog,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(changelogScrollState),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val buttonShape = RoundedCornerShape(8.dp)
            val updateButtonFilled = !isTelevision || updateFocused
            val updateButtonBackground = if (updateButtonFilled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            val updateButtonContentColor = if (updateButtonFilled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            if (!status.required) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(closeFocus)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            buttonShape
                        )
                        .tvFocusableClick(
                            onClick = { onEvent(UpdateState.Event.Dismiss) },
                            shape = buttonShape,
                            focusedScale = 1f,
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.update_later),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            if (autoUpdateSupported) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(updateFocus)
                        .background(updateButtonBackground, buttonShape)
                        .tvFocusableClick(
                            onClick = { onEvent(UpdateState.Event.ConfirmUpdate(status.apkUrl)) },
                            shape = buttonShape,
                            interactionSource = updateInteractionSource,
                            focusedScale = 1f,
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.update_install),
                        color = updateButtonContentColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        ManualUpdateHint(isTelevision = isTelevision)
    }
}

private fun String.formatReleaseNotes(): String =
    lines()
        .joinToString(separator = "\n") { line ->
            val trimmedEnd = line.trimEnd()
            val content = trimmedEnd.trimStart()
            val indent = trimmedEnd.take(trimmedEnd.length - content.length)
            when {
                content.startsWith("#") -> content.replace(Regex("^#{1,6}\\s*"), "")
                content.startsWith("* ") -> indent + "- " + content.removePrefix("* ")
                else -> trimmedEnd
            }
        }
        .replace(Regex("""\[(.*?)]\((.*?)\)"""), "$1")
        .replace("**", "")
        .replace("*", "")
        .replace("`", "")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

@Composable
private fun ManualUpdateHint(isTelevision: Boolean) {
    val releasesUrl = stringResource(R.string.update_manual_release_url)

    Column {
        Text(
            text = stringResource(R.string.update_manual_hint, releasesUrl),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!isTelevision) {
            val context = LocalContext.current
            TextButton(onClick = { context.openExternalUri(releasesUrl) }) {
                Text(text = stringResource(R.string.update_manual_open_releases))
            }
        }
    }
}

@Composable
private fun RequiredUpdateBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.update_required_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun UnsupportedAutoUpdateBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.update_auto_update_unsupported),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun DownloadingContent(status: UpdateState.State.Status.Downloading) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.update_downloading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(
            progress = { status.progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${(status.progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InstallingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.update_installing),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ErrorContent(
    status: UpdateState.State.Status.Error,
    onEvent: (UpdateState.Event) -> Unit,
) {
    val retryFocus = remember { FocusRequester() }
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(status.apkUrl) {
        runCatching {
            if (status.apkUrl != null) retryFocus.requestFocus() else closeFocus.requestFocus()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.update_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = status.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            status.apkUrl?.let { apkUrl ->
                Box(
                    modifier = Modifier
                        .focusRequester(retryFocus)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .tvFocusableClick(onClick = { onEvent(UpdateState.Event.RetryUpdate(apkUrl)) })
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.update_retry),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .focusRequester(closeFocus)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .tvFocusableClick(onClick = { onEvent(UpdateState.Event.Dismiss) })
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.update_close),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
