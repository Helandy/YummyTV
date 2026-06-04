@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun AccountTitle() {
    Text(
        text = stringResource(R.string.account_title),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}
