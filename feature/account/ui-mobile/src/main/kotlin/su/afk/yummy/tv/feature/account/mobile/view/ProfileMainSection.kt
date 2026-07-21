package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSex
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileMainSection(
    nickname: String,
    about: String,
    birthDate: String,
    sex: UserProfileSex,
    enabled: Boolean,
    onAboutChanged: (String) -> Unit,
    onBirthDateClick: () -> Unit,
    onSexChanged: (UserProfileSex) -> Unit,
) {
    AccountMobileSurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.profile_edit_main),
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_edit_nickname)) },
                readOnly = true,
                enabled = false,
            )
            OutlinedTextField(
                value = about,
                onValueChange = onAboutChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_edit_about)) },
                minLines = 3,
                enabled = enabled,
            )
            OutlinedTextField(
                value = birthDate,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onBirthDateClick),
                label = { Text(stringResource(R.string.profile_edit_birth_date)) },
                readOnly = true,
                enabled = enabled,
            )
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = it }) {
                OutlinedTextField(
                    value = sex.label(),
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_edit_sex)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    UserProfileSex.entries.forEach { value ->
                        DropdownMenuItem(
                            text = { Text(value.label()) },
                            onClick = { expanded = false; onSexChanged(value) },
                        )
                    }
                }
            }
        }
    }
}
