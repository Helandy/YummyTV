package su.afk.yummy.tv.feature.search.mobile.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.search.mobile.R

@Composable
internal fun SheetHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actionLabel: String? = null,
    actionVisible: Boolean = true,
    onClose: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.search_mobile_filters_back),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 80.dp),
        )
        if (actionLabel != null && onClose != null) {
            // The button is always composed so the header keeps a constant height;
            // visibility is animated with alpha only to avoid layout shifts below.
            val actionAlpha by animateFloatAsState(
                targetValue = if (actionVisible) 1f else 0f,
                animationSpec = tween(durationMillis = CHIP_ANIMATION_MILLIS),
                label = "sheetHeaderAction",
            )
            TextButton(
                onClick = onClose,
                enabled = actionVisible,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer { alpha = actionAlpha },
            ) {
                Text(actionLabel)
            }
        }
    }
}
