package su.afk.yummy.tv.feature.details.mobile.trailers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.video.TrailerWebPlayer
import su.afk.yummy.tv.core.designsystem.video.TrailerWebPlayerController
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.feature.details.presentation.R

/** Полноэкранный просмотр трейлера; при невозможности встроить видео предлагает внешний плеер. */
@Composable
fun TrailerPlayerMobileScreen(
    videoId: String?,
    embedUrl: String,
    externalUrl: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember { TrailerWebPlayerController() }
    var failed by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (failed) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.details_trailer_player_error),
                    color = Color.White,
                )
                Button(onClick = { context.openExternalUri(externalUrl) }) {
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
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}
