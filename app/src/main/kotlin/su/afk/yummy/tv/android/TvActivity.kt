package su.afk.yummy.tv.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import su.afk.yummy.tv.core.deeplink.api.DeepLinkHandler
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.feature.main.TvMainGraph
import su.afk.yummy.tv.feature.search.android.SystemSearchIntentHandler
import javax.inject.Inject
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@AndroidEntryPoint
class TvActivity : ComponentActivity() {

    @Inject
    lateinit var mainGraph: TvMainGraph

    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler

    @Inject
    lateinit var searchIntentHandler: SystemSearchIntentHandler

    @Inject
    lateinit var tvIntegration: ITvIntegration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        tvIntegration.bindBrowsableChannelRequests(this, lifecycleScope)

        setContent {
            // теги Compose видны UiAutomator как resource-id — на них опираются сценарии
            // baseline profile и бенчмарки (модуль :baselineprofile)
            Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                mainGraph.MainGraph()
            }
        }

        tvIntegration.start()
        handleIncomingIntent(intent)
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

}
