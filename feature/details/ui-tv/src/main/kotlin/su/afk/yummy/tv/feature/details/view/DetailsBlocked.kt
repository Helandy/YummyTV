package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvFocusableButton
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun DetailsBlocked(
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = TvScreenPadding.Horizontal,
                vertical = TvScreenPadding.Vertical,
            ),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.details_region_blocked),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(22.dp))
        TvFocusableButton(text = stringResource(R.string.details_back), onClick = onBack)
    }
}
