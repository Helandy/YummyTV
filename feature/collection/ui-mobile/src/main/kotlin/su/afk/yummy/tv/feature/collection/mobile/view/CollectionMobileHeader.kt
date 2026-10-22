package su.afk.yummy.tv.feature.collection.mobile.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.designsystem.utils.toBbAnnotatedString
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.feature.collection.presentation.R

private const val COLLAPSED_DESCRIPTION_LINES = 8

@Composable
internal fun CollectionMobileHeader(
    collection: CollectionDetail,
    isOwner: Boolean,
    isMutationLoading: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = collection.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (collection.description.isNotBlank()) {
            var expanded by rememberSaveable(collection.description) { mutableStateOf(false) }
            var isOverflowing by remember(collection.description) { mutableStateOf(false) }
            val linkColor = MaterialTheme.colorScheme.primary
            Text(
                text = remember(collection.description, linkColor) {
                    collection.description.toBbAnnotatedString(
                        linkStyle = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
                onTextLayout = { if (!expanded) isOverflowing = it.hasVisualOverflow },
            )
            if (isOverflowing || expanded) {
                Text(
                    text = stringResource(
                        if (expanded) R.string.collection_description_less else R.string.collection_description_mobile_more,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        if (isOwner) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                Button(onClick = onEdit, enabled = !isMutationLoading) {
                    Text(stringResource(su.afk.yummy.tv.feature.collection.presentation.R.string.collection_edit))
                }
                OutlinedButton(onClick = onDelete, enabled = !isMutationLoading) {
                    Text(stringResource(su.afk.yummy.tv.core.designsystem.R.string.common_delete))
                }
            }
        }
    }
}
