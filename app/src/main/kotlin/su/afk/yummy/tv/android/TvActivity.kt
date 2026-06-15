package su.afk.yummy.tv.android

import android.content.Intent
import android.media.tv.TvContract
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import su.afk.yummy.tv.android.search.SystemSearchIntentHandler
import su.afk.yummy.tv.core.analytics.StartupPerformanceTracker
import su.afk.yummy.tv.core.deeplink.DeepLinkHandler
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.feature.main.TvMainGraph
import javax.inject.Inject

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

    @Inject
    lateinit var startupPerformanceTracker: StartupPerformanceTracker

    private val requestChannelBrowsable = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { tvIntegration.refreshPreviewChannelStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startupPerformanceTracker.markUiActivityCreated(ACTIVITY_NAME)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            mainGraph.MainGraph()
        }
        window.decorView.doOnPreDraw {
            startupPerformanceTracker.markFirstFrame()
        }

        tvIntegration.start()
        lifecycleScope.launch {
            tvIntegration.browsableChannelRequest.collect { channelId ->
                runCatching {
                    requestChannelBrowsable.launch(
                        Intent(TvContract.ACTION_REQUEST_CHANNEL_BROWSABLE)
                            .putExtra(TvContract.EXTRA_CHANNEL_ID, channelId)
                    )
                }
            }
        }
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

    private companion object {
        const val ACTIVITY_NAME = "TvActivity"
    }
}
