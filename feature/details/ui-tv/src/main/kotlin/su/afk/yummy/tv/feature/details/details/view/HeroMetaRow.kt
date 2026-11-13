package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.common.anime.isReleasedAnimeStatus
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.utils.formatViews

/** Мета одной строкой через «·»: год, тип, статус, просмотры; возрастной рейтинг — единственная плашка. */
@Composable
internal fun HeroMetaRow(details: AnimeDetails) {
    val items = buildList {
        details.year?.let { add(it.toString()) }
        details.type?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    val status = details.status?.takeIf { it.isNotBlank() }
    val ageRating = details.ageRating?.takeIf { it.isNotBlank() }
    val viewsLabel = details.views?.formatViews()
    if (items.isEmpty() && status == null && ageRating == null && viewsLabel == null) return
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    val style = MaterialTheme.typography.bodyMedium
    val dotColor = textColor.copy(alpha = 0.5f)
    val dot: @Composable () -> Unit = { Text(text = META_SEPARATOR, style = style, color = dotColor) }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        ageRating?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        items.forEachIndexed { index, label ->
            if (index > 0) dot()
            Text(text = label, style = style, fontWeight = FontWeight.Medium, color = textColor)
        }
        status?.let { label ->
            if (items.isNotEmpty()) dot()
            val released = label.isReleasedAnimeStatus()
            Text(
                text = label,
                style = style,
                fontWeight = if (released) FontWeight.Medium else FontWeight.SemiBold,
                color = if (released) textColor else MaterialTheme.colorScheme.primary,
            )
        }
        viewsLabel?.let { views ->
            if (items.isNotEmpty() || status != null) dot()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(text = views, style = style, fontWeight = FontWeight.Medium, color = textColor)
            }
        }
    }
}

private const val META_SEPARATOR = "·"
