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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick

@Composable
internal fun LibraryTopTabItem(
    label: String,
    selected: Boolean,
    onSelected: () -> Unit,
    contentFocusRequester: FocusRequester,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                down = contentFocusRequester
                leftFocusRequester?.let { left = it }
                rightFocusRequester?.let { right = it }
            }
            .onFocusChanged { if (it.isFocused || it.hasFocus) onFocused() }
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                shape = shape,
            )
            .tvFocusableClick(onClick = onSelected, shape = shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
