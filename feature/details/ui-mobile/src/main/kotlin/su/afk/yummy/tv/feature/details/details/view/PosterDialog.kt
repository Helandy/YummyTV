package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.feature.details.details.utils.bestUrl
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun PosterDialog(
    details: AnimeDetails,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = details.poster.bestUrl(),
                contentDescription = details.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.details_mobile_close),
                    tint = Color.White,
                )
            }
        }
    }
}
