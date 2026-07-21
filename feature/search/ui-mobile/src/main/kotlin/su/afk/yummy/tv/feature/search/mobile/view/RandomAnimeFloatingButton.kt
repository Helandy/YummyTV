package su.afk.yummy.tv.feature.search.mobile.view

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.search.mobile.R

@Composable
internal fun RandomAnimeFloatingButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.search_mobile_random_anime)
    FloatingActionButton(
        onClick = { if (!isLoading) onClick() },
        modifier = Modifier
            .navigationBarsPadding()
            .semantics {
                contentDescription = description
                if (isLoading) disabled()
            },
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Casino,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
