package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection

@Composable
internal fun SettingsMobileApiContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection {
            OutlinedTextField(
                value = state.yaniApplicationToken,
                onValueChange = { onEvent(SettingsState.Event.YaniApplicationTokenChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                label = { Text(stringResource(R.string.settings_yani_application_token_label)) },
                placeholder = { Text(stringResource(R.string.settings_yani_application_token_placeholder)) },
                supportingText = { Text(stringResource(R.string.settings_yani_application_token_hint)) },
            )
        }
    }
}
