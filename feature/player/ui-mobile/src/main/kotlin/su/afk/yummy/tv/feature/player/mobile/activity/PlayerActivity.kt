package su.afk.yummy.tv.feature.player.mobile.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogHost
import su.afk.yummy.tv.core.designsystem.theme.YummyTvTheme
import su.afk.yummy.tv.feature.player.PlayerViewModel
import su.afk.yummy.tv.feature.player.host.PlayerActivityHost
import su.afk.yummy.tv.feature.player.host.PlayerHostNavigation
import su.afk.yummy.tv.feature.player.mobile.PlayerMobileScreen
import su.afk.yummy.tv.feature.player.mobile.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.navigator.PLAYER_CONTENT_KEY
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import javax.inject.Inject

/**
 * Мобильный плеер в отдельной Activity. Системный PiP сворачивает именно её окно, а
 * `MobileActivity` с вкладками остаётся под ним живой и доступной.
 */
@AndroidEntryPoint
class PlayerActivity : ComponentActivity(), PlayerActivityHost {

    @Inject
    lateinit var hostNavigation: PlayerHostNavigation

    private var destination: PlayerDestination? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        destination = savedInstanceState?.getString(STATE_DESTINATION)?.let(::decode)
            ?: decode(intent.getStringExtra(EXTRA_DESTINATION))
        if (destination == null) {
            finish()
            return
        }
        hostNavigation.attach(this)

        setContent {
            val dest = destination ?: return@setContent
            YummyTvTheme(isTelevision = false) {
                // Плеер — отдельная Activity: диалоги выбора (смена озвучки на торрент, «Продолжить») рисуем здесь.
                ChoiceDialogHost(isTelevision = false)
                val viewModel = hiltViewModel<PlayerViewModel, PlayerViewModel.Factory>(
                    key = PLAYER_CONTENT_KEY,
                    creationCallback = { factory -> factory.create(dest) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    PlayerMobileScreen(dest = dest, state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
    }

    /** Новое видео, пока плеер уже открыт (в том числе свёрнут в PiP): подменяем серию. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        decode(intent.getStringExtra(EXTRA_DESTINATION))?.let { destination = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        destination?.let { outState.putString(STATE_DESTINATION, encode(it)) }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        MobilePlayerPipController.enterIfPlaying(this)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        MobilePlayerPipController.updatePictureInPictureMode(isInPictureInPictureMode)
    }

    override fun onStop() {
        super.onStop()
        // Окно PiP закрыли крестиком: Activity остановилась, но не уничтожена - закрываем сами,
        // иначе плеер повис бы в фоне без окна.
        if (isInPictureInPictureMode) {
            finish()
        }
    }

    override fun onDestroy() {
        hostNavigation.detach(this)
        MobilePlayerPipController.updatePictureInPictureMode(false)
        super.onDestroy()
    }

    override fun onPlayerDestinationChanged(destination: PlayerDestination) {
        this.destination = destination
    }

    override fun finishPlayer() {
        finish()
    }

    // Android 12+: необработанный KEYCODE_CALL доходит до PhoneFallbackEventHandler, тот шлёт
    // CLOSE_SYSTEM_DIALOGS от имени приложения и система бросает SecurityException. Гасим клавишу.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        keyCode == KeyEvent.KEYCODE_CALL || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        keyCode == KeyEvent.KEYCODE_CALL || super.onKeyUp(keyCode, event)

    companion object {
        private const val EXTRA_DESTINATION = "su.afk.yummy.tv.feature.player.DESTINATION"
        private const val STATE_DESTINATION = "destination"

        private val json = Json { ignoreUnknownKeys = true }

        private fun encode(dest: PlayerDestination): String =
            json.encodeToString(PlayerDestination.serializer(), dest)

        private fun decode(raw: String?): PlayerDestination? =
            raw?.let { runCatching { json.decodeFromString(PlayerDestination.serializer(), it) }.getOrNull() }

        fun start(context: Context, destination: PlayerDestination) {
            val intent = Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_DESTINATION, encode(destination))
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
