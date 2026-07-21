package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.utils.formatViews

@Composable
internal fun HeroMetaRow(details: AnimeDetails) {
    val items = buildList {
        details.year?.let { add(it.toString()) }
        details.type?.let { add(it) }
        details.status?.let { add(it) }
        details.ageRating?.let { add(it) }
    }
    val viewsLabel = details.views?.formatViews()
    if (items.isEmpty() && viewsLabel == null) return
    val chipColor = MaterialTheme.colorScheme.onSurface
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { label ->
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = chipColor.copy(alpha = 0.85f),
                modifier = Modifier
                    .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        viewsLabel?.let { views ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = chipColor.copy(alpha = 0.85f),
                    modifier = Modifier
                        .height(10.dp)
                        .width(10.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = views,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = chipColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}
