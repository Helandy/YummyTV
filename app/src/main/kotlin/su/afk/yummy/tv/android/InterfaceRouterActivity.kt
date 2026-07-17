package su.afk.yummy.tv.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import su.afk.yummy.tv.android.view.InterfaceModeDialog
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummyTvTheme
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceModePreferences
import javax.inject.Inject

/**
 * Единая точка входа для обычного launcher, Android TV launcher и диплинков.
 *
 * При первом запуске предлагает выбрать интерфейс. После выбора всегда направляет исходный
 * Intent в [MobileActivity] или [TvActivity], не полагаясь на тип устройства из Configuration.
 */
@AndroidEntryPoint
class InterfaceRouterActivity : ComponentActivity() {

    @Inject
    lateinit var interfaceModePreferences: AppInterfaceModePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Отсутствие значения означает первый запуск после установки или очистки данных.
        val selectedMode = interfaceModePreferences.selectedMode
        if (selectedMode == null) {
            showInterfaceModeDialog()
        } else {
            // Сохранённый выбор позволяет сразу открыть нужный граф без повторного диалога.
            openSelectedInterface(selectedMode)
        }
    }

    private fun showInterfaceModeDialog() {
        setContent {
            YummyTvTheme {
                InterfaceModeDialog(
                    onTvSelected = { selectAndOpenInterface(AppInterfaceMode.TV) },
                    onMobileSelected = { selectAndOpenInterface(AppInterfaceMode.MOBILE) },
                )
            }
        }
    }

    private fun selectAndOpenInterface(mode: AppInterfaceMode) {
        interfaceModePreferences.select(mode)
        openSelectedInterface(mode)
    }

    private fun openSelectedInterface(mode: AppInterfaceMode) {
        val target = when (mode) {
            AppInterfaceMode.MOBILE -> MobileActivity::class.java
            AppInterfaceMode.TV -> TvActivity::class.java
        }
        val sourceIntent = intent
        // Не переносим launcher-флаги исходного Intent, но переиспользуем уже открытую Activity,
        // чтобы в одном task не появлялись два NavHost с общим NavigationManager.
        val targetIntent = Intent(this, target).apply {
            action = sourceIntent.action
            data = sourceIntent.data
            putExtras(sourceIntent)
            clipData = sourceIntent.clipData
            flags = (sourceIntent.flags and FORWARDED_URI_PERMISSION_FLAGS) or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(targetIntent)
        finish()
    }

    private companion object {
        const val FORWARDED_URI_PERMISSION_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }
}
