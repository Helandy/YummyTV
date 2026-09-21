package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft

@Composable
internal fun ApiSettingsPanel(
    token: String,
    upFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester? = null,
    restoreUpToTab: Boolean = true,
    onTokenChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_yani_application_token_label),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChanged,
            placeholder = { Text(stringResource(R.string.settings_yani_application_token_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (contentFocusRequester != null) {
                        Modifier.focusRequester(contentFocusRequester)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (restoreUpToTab) {
                        Modifier.restoreCategoryFocusOnLeft(upFocusRequester)
                    } else {
                        Modifier
                    },
                ),
        )
        Text(
            text = stringResource(R.string.settings_yani_application_token_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
