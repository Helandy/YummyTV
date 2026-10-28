package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.feature.library.presentation.R

/** Крестик удаления в углу карточки библиотеки ТВ — как на мобилке, но с фокусом для DPAD. */
@Composable
internal fun LibraryTvDeleteButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val error = MaterialTheme.colorScheme.error
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .tvFocusableClick(
                onClick = onClick,
                shape = CircleShape,
                interactionSource = interactionSource,
                focusedBorderColor = error,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.library_mobile_remove_content_description),
            tint = if (focused) error else error.copy(alpha = 0.72f),
            modifier = Modifier.size(22.dp),
        )
    }
}
