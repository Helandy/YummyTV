package su.afk.yummy.tv.feature.commonscreen.imageView.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.commonscreen.R

@Composable
internal fun NavigationControls(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = hasPrevious,
            modifier = Modifier.padding(start = 24.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.previous),
                tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp),
            )
        }

        IconButton(
            onClick = onNext,
            enabled = hasNext,
            modifier = Modifier.padding(end = 24.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.next),
                tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp),
            )
        }
    }
}
