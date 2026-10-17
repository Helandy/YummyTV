package su.afk.yummy.tv.feature.details.trailers

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.video.TrailerWebPlayer
import su.afk.yummy.tv.core.designsystem.video.TrailerWebPlayerController
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.feature.details.presentation.R

private const val SEEK_SECONDS = 10

/** Просмотр трейлера на ТВ: OK — пауза, влево/вправо — перемотка на 10 секунд, Назад — выход. */
@Composable
fun TrailerPlayerTvScreen(
    videoId: String?,
    embedUrl: String,
    externalUrl: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember { TrailerWebPlayerController() }
    val focusRequester = remember { FocusRequester() }
    val buttonFocusRequester = remember { FocusRequester() }
    var failed by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(failed) {
        // кадр до первой попытки: иначе скаффолд перебивает запрос фокуса
        androidx.compose.runtime.withFrameNanos { }
        (if (failed) buttonFocusRequester else focusRequester).requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (failed || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key.nativeKeyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    -> controller.togglePlayback()

                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND ->
                        controller.seekBy(-SEEK_SECONDS)

                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ->
                        controller.seekBy(SEEK_SECONDS)

                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .focusable(),
    ) {
        if (failed) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.details_trailer_player_error),
                    color = Color.White,
                )
                Button(
                    onClick = { context.openExternalUri(externalUrl) },
                    modifier = Modifier.focusRequester(buttonFocusRequester),
                ) {
                    Text(stringResource(R.string.details_trailer_open_external))
                }
            }
        } else {
            TrailerWebPlayer(
                videoId = videoId,
                embedUrl = embedUrl,
                controller = controller,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
