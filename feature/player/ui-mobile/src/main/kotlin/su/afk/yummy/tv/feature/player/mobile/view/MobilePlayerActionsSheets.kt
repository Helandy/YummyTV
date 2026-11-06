package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.feature.player.mobile.utils.labelRes

/**
 * Шторка «Действия» (детали, ориентация, PiP) и вложенная шторка выбора ориентации.
 * Открывается кнопкой «ещё» в нижней панели; вынесена из панели, чтобы пережить её автоскрытие.
 */
@Composable
internal fun MobilePlayerActionsSheets(
    showActions: Boolean,
    onActionsDismiss: () -> Unit,
    orientationMode: PlayerOrientationMode,
    onOrientationSelected: (PlayerOrientationMode) -> Unit,
    onDetails: () -> Unit,
    onPictureInPicture: () -> Unit,
    showDetails: Boolean,
    showPictureInPicture: Boolean,
    showCast: Boolean,
) {
    var showOrientationSheet by remember { mutableStateOf(false) }

    if (showActions) {
        PlayerMobileActionsSheet(
            showDetails = showDetails,
            showPictureInPicture = showPictureInPicture,
            showCast = showCast,
            orientationLabel = stringResource(orientationMode.labelRes),
            onDetails = onDetails,
            onOrientation = { showOrientationSheet = true },
            onPictureInPicture = onPictureInPicture,
            onDismiss = onActionsDismiss,
        )
    }

    if (showOrientationSheet) {
        MobilePlayerOrientationSheet(
            selected = orientationMode,
            onSelected = { mode ->
                showOrientationSheet = false
                onOrientationSelected(mode)
            },
            onDismiss = { showOrientationSheet = false },
        )
    }
}
