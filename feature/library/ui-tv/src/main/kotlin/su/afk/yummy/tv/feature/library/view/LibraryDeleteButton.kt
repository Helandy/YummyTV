package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.feature.library.R

@Composable
internal fun LibraryDeleteButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    val containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.92f)
    Box(
        modifier = modifier
            .background(containerColor, shape)
            .tvFocusableClick(onClick = onClick, shape = shape, focusedScale = 1f)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.library_delete),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onError,
            fontWeight = FontWeight.Medium,
        )
    }
}
