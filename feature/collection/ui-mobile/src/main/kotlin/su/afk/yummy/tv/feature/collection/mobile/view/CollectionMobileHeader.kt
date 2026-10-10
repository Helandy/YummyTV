package su.afk.yummy.tv.feature.collection.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.domain.collection.model.CollectionDetail

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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (collection.description.isNotBlank()) {
            Text(
                text = collection.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
            )
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
