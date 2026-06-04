package su.afk.yummy.tv.feature.details.screenshots.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.screenshots.ScreenshotsState

@Composable
internal fun ScreenshotPreview(
    state: ScreenshotsState.State,
    index: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val screenshots = state.screenshots
    val screenshot = screenshots.getOrNull(index)
    val arrowFocusRequester = remember { FocusRequester() }
    val hasNext = index < screenshots.lastIndex
    val hasPrevious = index > 0
    LaunchedEffect(index, hasNext, hasPrevious) {
        if (hasNext || hasPrevious) {
            arrowFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = screenshot?.full ?: screenshot?.small,
            contentDescription = screenshot?.episode?.let {
                stringResource(R.string.details_episode_content_description, it)
            },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        if (hasPrevious) {
            PreviewIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.details_previous),
                alignment = Alignment.CenterStart,
                onClick = onPrevious,
                focusRequester = if (!hasNext) arrowFocusRequester else null,
            )
        }

        if (hasNext) {
            PreviewIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.details_next),
                alignment = Alignment.CenterEnd,
                onClick = onNext,
                focusRequester = arrowFocusRequester,
            )
        }

        Text(
            text = "${index + 1} / ${screenshots.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
