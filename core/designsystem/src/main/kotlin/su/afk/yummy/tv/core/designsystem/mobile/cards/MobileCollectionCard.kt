package su.afk.yummy.tv.core.designsystem.mobile.cards

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.PostersCollage
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer

private const val COLLAGE_ASPECT_RATIO = 2.4f

/** Плоская карточка коллекции: коллаж из трёх постеров, [badge] в верхнем правом углу и название ниже. */
@Composable
fun MobileCollectionCard(
    title: String,
    posterUrls: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            Modifier
                .clickablePointer()
                .combinedClickable(onClick = onClick),
        ) {
            PostersCollage(
                posterUrls = posterUrls,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COLLAGE_ASPECT_RATIO),
                overlay = { badge?.invoke(this) },
            )
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
