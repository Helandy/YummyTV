package su.afk.yummy.tv.android

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import su.afk.yummy.tv.core.deeplink.DeepLinkHandler
import su.afk.yummy.tv.feature.main.MobileMainGraph
import su.afk.yummy.tv.feature.player.pip.MobilePlayerPipController
import javax.inject.Inject

@AndroidEntryPoint
class MobileActivity : ComponentActivity() {

    @Inject lateinit var mainGraph: MobileMainGraph
    @Inject lateinit var deepLinkHandler: DeepLinkHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            mainGraph.MainGraph()
        }

        deepLinkHandler.handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkHandler.handle(intent)
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
}
