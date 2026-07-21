package su.afk.yummy.tv.feature.account.mobile.profileedit.utils

import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.profileedit.ProfileEditState

internal fun ProfileEditState.MessageType.messageRes() = when (this) {
    ProfileEditState.MessageType.PROFILE_SAVED -> R.string.profile_saved
    ProfileEditState.MessageType.PROFILE_SAVE_FAILED -> R.string.profile_save_failed
    ProfileEditState.MessageType.IMAGE_SAVED -> R.string.profile_image_saved
    ProfileEditState.MessageType.IMAGE_SAVE_FAILED -> R.string.profile_image_save_failed
    ProfileEditState.MessageType.PASSWORD_CHANGED -> R.string.profile_password_changed
    ProfileEditState.MessageType.PASSWORD_CHANGE_FAILED -> R.string.profile_password_change_failed
    ProfileEditState.MessageType.ACCOUNT_UNLINKED -> R.string.profile_account_unlinked
    ProfileEditState.MessageType.ACCOUNT_UNLINK_FAILED -> R.string.profile_account_unlink_failed
}
