package su.afk.yummy.tv.core.designsystem.tv

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.MarqueeTitleText
import su.afk.yummy.tv.core.designsystem.components.PostersCollage
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick

private const val COLLAGE_ASPECT_RATIO = 2.6f

/** Плоская карточка коллекции: коллаж из трёх постеров, [badge] в верхнем правом углу и название ниже. */
@Composable
fun TvCollectionCard(
    title: String,
    posterUrls: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.medium
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                val focused = focusState.isFocused || focusState.hasFocus
                if (focused && !isFocused) onFocused()
                isFocused = focused
            }
            .tvFocusableClick(onClick = onClick, shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column {
            PostersCollage(
                posterUrls = posterUrls,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COLLAGE_ASPECT_RATIO),
                overlay = { badge?.invoke(this) },
            )
            MarqueeTitleText(
                text = title,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                minLines = 1,
                maxLines = 1,
                isFocused = isFocused,
            )
        }
    }
}
