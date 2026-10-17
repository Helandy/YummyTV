package su.afk.yummy.tv.feature.catalog.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.catalog.presentation.R

/** Кнопка очистки строки поиска; оформление как у [FilterButton]. */
@Composable
internal fun ClearSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme

    Button(
        onClick = onClick,
        modifier = modifier.size(CatalogTvHeaderButtonHeight),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (focused) colors.primary else Color.Transparent,
            contentColor = if (focused) colors.onPrimary else colors.onSurface,
        ),
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) colors.primary else colors.outline,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.search_clear),
        )
    }
}
