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
            ChoiceDialogState.Loading -> CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 24.dp).align(Alignment.CenterHorizontally),
            )

            is ChoiceDialogState.Message -> Text(
                text = current.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            is ChoiceDialogState.Items -> current.items.forEach { item ->
                ChoiceRow(item = item, onClick = { onAnswer(item.id) })
            }
        }
        ChoiceCancelRow(text = request.cancelText, onClick = { onAnswer(null) })
    }
}

@Composable
private fun ChoiceRow(item: ChoiceDialogItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChoiceCancelRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
