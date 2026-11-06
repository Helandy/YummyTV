package su.afk.yummy.tv.android

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import su.afk.yummy.tv.android.episodepush.utils.cancelEpisodePushNotification
import su.afk.yummy.tv.android.shortcuts.AppShortcutsPublisher
import su.afk.yummy.tv.android.widget.ContinueWatchingWidgetUpdater
import su.afk.yummy.tv.core.deeplink.api.DeepLinkHandler
import su.afk.yummy.tv.core.designsystem.locals.LocalStaticBaseUrl
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.preferences.interface_language.AppInterfaceLanguageContext
import su.afk.yummy.tv.feature.catalog.android.SystemSearchIntentHandler
import su.afk.yummy.tv.feature.main.mobile.MobileMainGraph
import su.afk.yummy.tv.feature.main.mobile.navigation.MobileKeyboardShortcuts
import javax.inject.Inject

@AndroidEntryPoint
class MobileActivity : ComponentActivity() {

    @Inject
    lateinit var mainGraph: MobileMainGraph

    @Inject
    lateinit var siteApiConfig: SiteApiConfig

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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppInterfaceLanguageContext.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // тёмные бары до первой композиции; YummyTvTheme затем подстраивает иконки под выбранный фон
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            // теги Compose видны UiAutomator как resource-id — на них опираются сценарии
            // baseline profile и бенчмарки (модуль :baselineprofile)
            CompositionLocalProvider(LocalStaticBaseUrl provides siteApiConfig.staticBaseUrl) {
                Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                    mainGraph.MainGraph()
                }
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
        intent.cancelEpisodePushNotification(this)
        if (!searchIntentHandler.handle(intent)) {
            deepLinkHandler.handle(intent)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        keyboardShortcuts.handle(event, onBack = onBackPressedDispatcher::onBackPressed) ||
            super.onKeyDown(keyCode, event)
}
