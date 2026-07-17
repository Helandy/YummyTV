package su.afk.yummy.tv.feature.account.profileedit

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.profileedit.utils.prepareProfileImage
import su.afk.yummy.tv.feature.account.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.view.LinkedAccountsSection
import su.afk.yummy.tv.feature.account.view.ProfileMainSection
import su.afk.yummy.tv.feature.account.view.ProfileMediaSection
import su.afk.yummy.tv.feature.account.view.ProfilePasswordSection
import su.afk.yummy.tv.feature.account.view.ProfilePrivacySection
import su.afk.yummy.tv.feature.account.view.label
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import su.afk.yummy.tv.core.designsystem.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditMobileScreen(
    state: ProfileEditState.State,
    effect: Flow<ProfileEditState.Effect>,
    onEvent: (ProfileEditState.Event) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImageKind by remember { mutableStateOf<ProfileImageKind?>(null) }
    var deleteImageKind by remember { mutableStateOf<ProfileImageKind?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            val kind = pendingImageKind
            pendingImageKind = null
            if (uri != null && kind != null) {
                scope.launch {
                    val bytes = prepareProfileImage(context, uri, kind)
                    if (bytes != null) {
                        onEvent(ProfileEditState.Event.ImageSelected(kind, bytes, uri.toString()))
                    } else {
                        Toast.makeText(
                            context,
                            R.string.profile_image_prepare_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    LaunchedEffect(effect) {
        effect.collect { event ->
            if (event is ProfileEditState.Effect.Message) {
                Toast.makeText(context, event.type.messageRes(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.profile_edit_title),
                onBack = { onEvent(ProfileEditState.Event.BackSelected) },
            )
        },
    ) {
        when {
            state.isLoading -> AccountMobileLoadingIndicator()
            state.hasLoadError -> MobileMessage(
                title = stringResource(R.string.profile_edit_load_error),
                icon = Icons.Default.PersonOff,
                actionLabel = stringResource(CoreR.string.retry),
                onAction = { onEvent(ProfileEditState.Event.RetrySelected) },
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "media") {
                    ProfileMediaSection(
                        avatarUrl = state.pendingAvatarPreview ?: state.avatarUrl,
                        bannerUrl = state.pendingBannerPreview ?: state.bannerUrl,
                        enabled = !state.isImageLoading,
                        onPick = { kind ->
                            pendingImageKind = kind
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onDelete = { deleteImageKind = it },
                    )
                }
                item(key = "main") {
                    ProfileMainSection(
                        nickname = state.nickname,
                        about = state.about,
                        birthDate = state.birthDate,
                        sex = state.sex,
                        enabled = !state.isSaving,
                        onAboutChanged = { onEvent(ProfileEditState.Event.AboutChanged(it)) },
                        onBirthDateClick = { showDatePicker = true },
                        onSexChanged = { onEvent(ProfileEditState.Event.SexChanged(it)) },
                    )
                }
                item(key = "privacy") {
                    ProfilePrivacySection(
                        listPrivacy = state.listPrivacy,
                        showShikimori = state.showShikimori,
                        showTelegram = state.showTelegram,
                        showVk = state.showVk,
                        showDiscord = state.showDiscord,
                        notifyTelegram = state.notifyTelegram,
                        notifyVk = state.notifyVk,
                        enabled = !state.isSaving,
                        onListPrivacyChanged = {
                            onEvent(
                                ProfileEditState.Event.ListPrivacyChanged(
                                    it
                                )
                            )
                        },
                        onShowShikimoriChanged = {
                            onEvent(
                                ProfileEditState.Event.ShowShikimoriChanged(
                                    it
                                )
                            )
                        },
                        onShowTelegramChanged = {
                            onEvent(
                                ProfileEditState.Event.ShowTelegramChanged(
                                    it
                                )
                            )
                        },
                        onShowVkChanged = { onEvent(ProfileEditState.Event.ShowVkChanged(it)) },
                        onShowDiscordChanged = {
                            onEvent(
                                ProfileEditState.Event.ShowDiscordChanged(
                                    it
                                )
                            )
                        },
                        onNotifyTelegramChanged = {
                            onEvent(
                                ProfileEditState.Event.NotifyTelegramChanged(
                                    it
                                )
                            )
                        },
                        onNotifyVkChanged = { onEvent(ProfileEditState.Event.NotifyVkChanged(it)) },
                    )
                }
                item(key = "save") {
                    Button(
                        onClick = { onEvent(ProfileEditState.Event.SaveSelected) },
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.profile_edit_save)) }
                }
                item(key = "password") {
                    ProfilePasswordSection(
                        oldPassword = state.oldPassword,
                        newPassword = state.newPassword,
                        confirmPassword = state.confirmPassword,
                        isSaving = state.isPasswordSaving,
                        validationError = state.passwordValidationError,
                        onOldPasswordChanged = {
                            onEvent(
                                ProfileEditState.Event.OldPasswordChanged(
                                    it
                                )
                            )
                        },
                        onNewPasswordChanged = {
                            onEvent(
                                ProfileEditState.Event.NewPasswordChanged(
                                    it
                                )
                            )
                        },
                        onConfirmPasswordChanged = {
                            onEvent(
                                ProfileEditState.Event.ConfirmPasswordChanged(
                                    it
                                )
                            )
                        },
                        onSave = { onEvent(ProfileEditState.Event.ChangePasswordSelected) },
                    )
                }
                item(key = "linked_accounts") {
                    LinkedAccountsSection(
                        linkedAccounts = state.linkedAccounts,
                        unlinkingAccount = state.unlinkingAccount,
                        onUnlink = { onEvent(ProfileEditState.Event.UnlinkAccountSelected(it)) },
                    )
                }
            }
        }
    }

    deleteImageKind?.let { kind ->
        AlertDialog(
            onDismissRequest = { deleteImageKind = null },
            title = { Text(stringResource(R.string.profile_delete_image_title)) },
            text = { Text(stringResource(R.string.profile_delete_image_message)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteImageKind = null
                    onEvent(ProfileEditState.Event.DeleteImageSelected(kind))
                }) { Text(stringResource(R.string.profile_delete_image_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteImageKind = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    state.pendingUnlinkAccount?.let { provider ->
        AlertDialog(
            onDismissRequest = { onEvent(ProfileEditState.Event.UnlinkAccountDismissed) },
            title = {
                Text(
                    stringResource(
                        R.string.profile_unlink_confirm_title,
                        provider.label()
                    )
                )
            },
            text = { Text(stringResource(R.string.profile_unlink_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ProfileEditState.Event.UnlinkAccountConfirmed) },
                    enabled = state.unlinkingAccount == null,
                ) { Text(stringResource(R.string.profile_unlink_account)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(ProfileEditState.Event.UnlinkAccountDismissed) },
                    enabled = state.unlinkingAccount == null,
                ) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    if (showDatePicker) {
        val initialMillis = remember(state.birthDate) {
            runCatching {
                LocalDate.parse(state.birthDate).atStartOfDay(ZoneOffset.UTC).toInstant()
                    .toEpochMilli()
            }
                .getOrNull()
        }
        val dateState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onEvent(ProfileEditState.Event.BirthDateChanged(date.toString()))
                    }
                    showDatePicker = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        ) { DatePicker(state = dateState) }
    }
}

private fun ProfileEditState.MessageType.messageRes() = when (this) {
    ProfileEditState.MessageType.PROFILE_SAVED -> R.string.profile_saved
    ProfileEditState.MessageType.PROFILE_SAVE_FAILED -> R.string.profile_save_failed
    ProfileEditState.MessageType.IMAGE_SAVED -> R.string.profile_image_saved
    ProfileEditState.MessageType.IMAGE_SAVE_FAILED -> R.string.profile_image_save_failed
    ProfileEditState.MessageType.PASSWORD_CHANGED -> R.string.profile_password_changed
    ProfileEditState.MessageType.PASSWORD_CHANGE_FAILED -> R.string.profile_password_change_failed
    ProfileEditState.MessageType.ACCOUNT_UNLINKED -> R.string.profile_account_unlinked
    ProfileEditState.MessageType.ACCOUNT_UNLINK_FAILED -> R.string.profile_account_unlink_failed
}
