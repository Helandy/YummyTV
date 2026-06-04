@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.AccountState
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun CaptchaChallenge(
    state: AccountState.State,
    onSolved: (String) -> Unit,
    onExpired: () -> Unit,
    onFailed: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.account_captcha_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = if (state.isLoading) {
                stringResource(R.string.account_captcha_loading)
            } else {
                stringResource(R.string.account_captcha_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HCaptchaWebView(
            siteKey = state.captchaSiteKey,
            onSolved = onSolved,
            onExpired = onExpired,
            onFailed = onFailed,
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                ),
        )
        ErrorText(state.captchaError)
    }
}
