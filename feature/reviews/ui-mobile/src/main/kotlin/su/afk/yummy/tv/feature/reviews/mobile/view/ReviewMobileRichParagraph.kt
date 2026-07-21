package su.afk.yummy.tv.feature.reviews.mobile.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import su.afk.yummy.tv.feature.reviews.model.ReviewContentBlock
import su.afk.yummy.tv.feature.reviews.utils.reviewAnnotatedText

@Composable
internal fun ReviewMobileRichParagraph(block: ReviewContentBlock.Paragraph) {
    val text = remember(block) { reviewAnnotatedText(block) }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
