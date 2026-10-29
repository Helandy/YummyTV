package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R

private val StarYellow = Color(0xFFFFC107)

/** Кнопка-звезда озвучки или балансера: помеченные звездой показываются в списках выбора первыми. */
@Composable
fun SourceStarButton(
    starred: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        SourceStarMark(starred = starred, alwaysVisible = true)
    }
}

/** Значок звезды без нажатия: для ТВ, где звезда переключается долгим нажатием на строку. */
@Composable
fun SourceStarMark(
    starred: Boolean,
    modifier: Modifier = Modifier,
    alwaysVisible: Boolean = false,
) {
    if (!starred && !alwaysVisible) return
    Icon(
        imageVector = if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
        contentDescription = stringResource(
            if (starred) R.string.source_star_remove else R.string.source_star_add,
        ),
        tint = if (starred) {
            StarYellow
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier,
    )
}
