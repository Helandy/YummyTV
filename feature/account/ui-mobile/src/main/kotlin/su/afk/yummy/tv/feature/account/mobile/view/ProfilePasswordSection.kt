package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun ProfilePasswordSection(
    oldPassword: String,
    newPassword: String,
    confirmPassword: String,
    isSaving: Boolean,
    validationError: Boolean,
    onOldPasswordChanged: (String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    AccountMobileSurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.profile_edit_password),
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = oldPassword,
                onValueChange = onOldPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_old_password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isSaving,
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_new_password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isSaving,
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_confirm_password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isSaving,
                isError = validationError,
                supportingText = if (validationError) {
                    { Text(stringResource(R.string.profile_password_validation_error)) }
                } else null,
            )
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_change_password))
            }
        }
    }
}
