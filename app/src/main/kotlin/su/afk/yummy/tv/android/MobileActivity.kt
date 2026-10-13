package su.afk.yummy.tv.android

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import su.afk.yummy.tv.android.shortcuts.AppShortcutsPublisher
import su.afk.yummy.tv.android.widget.ContinueWatchingWidgetUpdater
import su.afk.yummy.tv.core.deeplink.api.DeepLinkHandler
import su.afk.yummy.tv.feature.main.mobile.MobileMainGraph
import su.afk.yummy.tv.feature.main.mobile.navigation.MobileKeyboardShortcuts
import su.afk.yummy.tv.feature.player.mobile.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.search.android.SystemSearchIntentHandler
import javax.inject.Inject

@AndroidEntryPoint
class MobileActivity : ComponentActivity() {

    @Inject
    lateinit var mainGraph: MobileMainGraph

    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler

    @Inject
    lateinit var searchIntentHandler: SystemSearchIntentHandler

    @Inject
    lateinit var keyboardShortcuts: MobileKeyboardShortcuts

    @Inject
    lateinit var appShortcutsPublisher: AppShortcutsPublisher

    @Inject
    lateinit var continueWatchingWidgetUpdater: ContinueWatchingWidgetUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // теги Compose видны UiAutomator как resource-id — на них опираются сценарии
            // baseline profile и бенчмарки (модуль :baselineprofile)
            Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                mainGraph.MainGraph()
            }
        }

        handleIncomingIntent(intent)

        // Ярлыки и виджет — только мобильный интерфейс; обновляются, пока открыто приложение.
        lifecycleScope.launch { appShortcutsPublisher.run() }
        lifecycleScope.launch { continueWatchingWidgetUpdater.run() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        if (!searchIntentHandler.handle(intent)) {
            deepLinkHandler.handle(intent)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        keyboardShortcuts.handle(event, onBack = onBackPressedDispatcher::onBackPressed) ||
            super.onKeyDown(keyCode, event)

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
}
