package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.details.model.MobilePickerItem
import su.afk.yummy.tv.feature.details.view.common.formatCompactCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePickerBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            content()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun MobilePickerItems(
    items: List<MobilePickerItem>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        contentPadding = PaddingValues(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.key }) { item ->
            MobilePickerItemRow(item)
        }
    }
}

@Composable
private fun MobilePickerItemRow(item: MobilePickerItem) {
    val colorScheme = MaterialTheme.colorScheme
    val background = if (item.enabled) {
        colorScheme.surfaceVariant.copy(alpha = 0.72f)
    } else {
        colorScheme.surfaceVariant.copy(alpha = 0.34f)
    }
    val titleColor = if (item.enabled) colorScheme.onSurface else colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = item.enabled, onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = item.title,
            color = titleColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (item.enabled) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!item.subtitle.isNullOrBlank()) {
            val subtitleColor =
                colorScheme.onSurfaceVariant.copy(alpha = if (item.enabled) 1f else 0.6f)
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val views = item.views
                if (views != null && views > 0) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = subtitleColor,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = views.formatCompactCount(),
                        style = MaterialTheme.typography.labelSmall,
                        color = subtitleColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
