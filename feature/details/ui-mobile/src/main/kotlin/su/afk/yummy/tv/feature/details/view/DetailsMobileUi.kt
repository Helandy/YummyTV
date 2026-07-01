package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
internal fun DetailsMediaCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    footerText: String? = null,
    footerTextColor: Color = Color.Unspecified,
    secondaryFooterText: String? = null,
    secondaryFooterTextColor: Color = Color.Unspecified,
    badge: String? = null,
    leadingIcon: ImageVector? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    mediaWeight: Float = 0.42f,
    mediaAspectRatio: Float = 1.45f,
    mediaProgress: Float? = null,
    mediaProgressColor: Color = Color.Unspecified,
    mediaTopEndContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val contentWeight = 1f - mediaWeight
    val resolvedMediaProgressColor = if (mediaProgressColor == Color.Unspecified) {
        MaterialTheme.colorScheme.primary
    } else {
        mediaProgressColor
    }
    val resolvedFooterTextColor = if (footerTextColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
    } else {
        footerTextColor
    }
    val resolvedSecondaryFooterTextColor = if (secondaryFooterTextColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
    } else {
        secondaryFooterTextColor
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(mediaWeight)
                    .aspectRatio(mediaAspectRatio)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(Color.Black.copy(alpha = 0.52f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                if (!badge.isNullOrBlank()) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
                mediaTopEndContent?.let { content ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    ) {
                        content()
                    }
                }
                mediaProgress?.let { progress ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(resolvedMediaProgressColor.copy(alpha = 0.25f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(resolvedMediaProgressColor),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .weight(contentWeight)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!footerText.isNullOrBlank()) {
                        Text(
                            text = footerText,
                            style = MaterialTheme.typography.labelSmall,
                            color = resolvedFooterTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!secondaryFooterText.isNullOrBlank()) {
                        Text(
                            text = secondaryFooterText,
                            style = MaterialTheme.typography.labelSmall,
                            color = resolvedSecondaryFooterTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                trailingAction?.invoke()
            }
        }
    }
}

internal val DetailsPlayIcon: ImageVector = Icons.Filled.PlayArrow
