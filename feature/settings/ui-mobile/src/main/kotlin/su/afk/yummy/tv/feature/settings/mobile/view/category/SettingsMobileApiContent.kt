package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.utils.addUrlLinks
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.common.utils.label

@Composable
internal fun SettingsMobileApiContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection {
            OutlinedTextField(
                shape = MaterialTheme.shapes.large,
                value = state.yaniApplicationToken,
                onValueChange = { onEvent(SettingsState.Event.YaniApplicationTokenChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                label = { Text(stringResource(R.string.settings_yani_application_token_label)) },
                placeholder = { Text(stringResource(R.string.settings_yani_application_token_placeholder)) },
                supportingText = {
                    val hint = stringResource(R.string.settings_yani_application_token_hint)
                    val linkStyle = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    )
                    Text(buildAnnotatedString {
                        append(hint)
                        addUrlLinks(hint, linkStyle)
                    })
                },
            )
            if (state.isYaniApplicationTokenCustom) {
                TextButton(
                    onClick = { onEvent(SettingsState.Event.YaniApplicationTokenChanged("")) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_yani_application_token_reset))
                }
            }
        }
    }
}
