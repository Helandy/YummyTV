package su.afk.yummy.tv.feature.player.service

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

@Composable
internal fun rememberMobileMediaController(): MediaController {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MobilePlayerMediaSessionService::class.java),
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener(
            {
                controller = runCatching { controllerFuture.get() }.getOrNull()
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            controller = null
            MediaController.releaseFuture(controllerFuture)
        }
    }

    return controller
}
