package su.afk.yummy.tv.core.designsystem.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet

/** Мобильный вид запроса подтверждения: шторка поверх текущего экрана, строки как в пикерах деталки. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileConfirmSheet(
    request: ConfirmDialogRequest,
    onAnswer: (Boolean) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    BaseBottomSheet(
        onDismissRequest = { onAnswer(false) },
        title = request.title,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = request.message,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        ConfirmRow(
            text = request.confirmText,
            titleColor = colorScheme.primary,
            testTag = "confirm_dialog_accept",
            onClick = { onAnswer(true) },
        )
        ConfirmRow(
            text = request.dismissText,
            titleColor = colorScheme.onSurface,
            testTag = "confirm_dialog_dismiss",
            onClick = { onAnswer(false) },
        )
    }
}

@Composable
private fun ConfirmRow(
    text: String,
    titleColor: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
