package su.afk.yummy.tv.feature.details.screenshots.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick

@Composable
internal fun PreviewIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    alignment: Alignment,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(alignment)
            .padding(24.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocusableClick(onClick = onClick, shape = CircleShape)
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .padding(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}
