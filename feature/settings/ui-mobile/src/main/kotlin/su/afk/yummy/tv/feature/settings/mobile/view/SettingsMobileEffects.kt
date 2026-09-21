package su.afk.yummy.tv.feature.settings.mobile.view

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.utils.system.restartApplication
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.R

/** Обработка одноразовых эффектов настроек: рестарт после смены интерфейса и выбор папки экспорта. */
@Composable
internal fun SettingsMobileEffects(
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    val context = LocalContext.current
    val videoExportDirectoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            onEvent(SettingsState.Event.VideoExportDirectoryGranted(it.toString()))
        }
    }

    LaunchedEffect(Unit) {
        effect.collect { settingsEffect ->
            when (settingsEffect) {
                SettingsState.Effect.RestartApplication -> {
                    if (!context.restartApplication()) {
                        Toast.makeText(
                            context,
                            R.string.settings_interface_restart_failed,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                SettingsState.Effect.OpenVideoExportDirectoryPicker ->
                    videoExportDirectoryPicker.launch(null)

                SettingsState.Effect.VideoExportDirectorySelectionFailed ->
                    Toast.makeText(
                        context,
                        R.string.settings_video_export_directory_error,
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }
}
