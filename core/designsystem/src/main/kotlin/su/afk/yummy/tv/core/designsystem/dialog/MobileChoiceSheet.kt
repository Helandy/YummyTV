package su.afk.yummy.tv.core.designsystem.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet

/** Мобильный вид выбора из списка: шторка с прокручиваемым списком строк. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileChoiceSheet(
    request: ChoiceDialogRequest,
    onAnswer: (String?) -> Unit,
) {
    val state by request.state.collectAsState()
    BaseBottomSheet(
        onDismissRequest = { onAnswer(null) },
        title = request.title,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        scrollableContent = true,
    ) {
        when (val current = state) {
            ChoiceDialogState.Loading, is ChoiceDialogState.AutoSelect -> CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 24.dp).align(Alignment.CenterHorizontally),
            )

            is ChoiceDialogState.Progress -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 24.dp).align(Alignment.CenterHorizontally),
                )
                Text(
                    text = current.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally),
                )
            }

            is ChoiceDialogState.Message -> {
                Text(
                    text = current.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                current.retryText?.let { retryText ->
                    ChoiceActionRow(
                        text = retryText,
                        filled = true,
                        onClick = { onAnswer(ChoiceDialogCenter.RETRY_ID) },
                    )
                }
            }

            is ChoiceDialogState.Items -> current.items.forEach { item ->
                ChoiceRow(item = item, onClick = { onAnswer(item.id) })
            }
        }
        ChoiceActionRow(text = request.cancelText, filled = false, onClick = { onAnswer(null) })
    }
}

@Composable
private fun ChoiceRow(item: ChoiceDialogItem, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        ChoiceBadges(
            badges = item.badges,
            markers = item.markers,
            warnings = item.warnings,
            chipColor = colors.onSurface.copy(alpha = 0.10f),
            onChipColor = colors.onSurface.copy(alpha = 0.85f),
            accentColor = colors.primary.copy(alpha = 0.22f),
            onAccentColor = colors.primary,
        )
        ChoiceStats(stats = item.stats, neutralColor = colors.onSurfaceVariant)
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** Кнопка внизу диалога: «Повторить» ([filled], акцентная) или «Отмена». */
@Composable
private fun ChoiceActionRow(text: String, filled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Normal,
        color = if (filled) colors.onPrimary else colors.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) colors.primary else colors.surfaceVariant.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
