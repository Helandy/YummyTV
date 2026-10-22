package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import su.afk.yummy.tv.core.designsystem.R

@Composable
fun GithubStarIcon(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.ic_github_star),
        contentDescription = null,
        modifier = modifier,
    )
}
