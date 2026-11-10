package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.feature.details.presentation.R

/**
 * Последняя строка ленты деталей: ссылка на комментарии и справа кнопка «Вернуться к поиску»,
 * чтобы из конца длинной ленты начать новый поиск, не возвращаясь назад по цепочке тайтлов.
 */
@Composable
internal fun DetailsCommentsRow(
    commentsBadge: String?,
    onCommentsClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Собственные горизонтальные отступы ссылки дают и левый край, как у соседних строк, и зазор до кнопки.
        DetailsLinkRow(
            label = stringResource(R.string.details_comments),
            icon = Icons.AutoMirrored.Filled.Comment,
            badge = commentsBadge,
            onClick = onCommentsClick,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .padding(end = DetailsSectionHorizontalPadding, top = 4.dp, bottom = 4.dp)
                .tvFocusableClick(onClick = onSearchClick, shape = shape, focusedScale = 1.01f)
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .heightIn(min = 64.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = stringResource(R.string.details_back_to_search),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
