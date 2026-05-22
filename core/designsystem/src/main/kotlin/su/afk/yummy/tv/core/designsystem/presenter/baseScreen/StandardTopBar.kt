package su.afk.yummy.tv.core.designsystem.presenter.baseScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    content: @Composable ColumnScope.() -> Unit,
    topBarWindowInsets: WindowInsets,
) {
    val compensateEnd = 16.dp

    TopAppBar(
        windowInsets = topBarWindowInsets,
        scrollBehavior = scrollBehavior,
        title = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(end = compensateEnd)
            ) {
                content()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
