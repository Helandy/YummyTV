package su.afk.yummy.tv.android

import android.content.Intent
import android.media.tv.TvContract
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.deeplink.DeepLinkHandler
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.feature.main.api.IMainGraph
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var mainGraph: IMainGraph
    @Inject lateinit var deepLinkHandler: DeepLinkHandler
    @Inject lateinit var tvIntegration: ITvIntegration

    private val requestChannelBrowsable = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { tvIntegration.refreshPreviewChannelStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            mainGraph.MainGraph()
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
        deepLinkHandler.handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkHandler.handle(intent)
    }
}
