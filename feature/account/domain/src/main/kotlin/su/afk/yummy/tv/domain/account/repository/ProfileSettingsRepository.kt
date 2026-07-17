package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.EditableProfile
import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.domain.account.model.ProfileUpdate

interface ProfileSettingsRepository {
    suspend fun getProfile(): EditableProfile
    suspend fun updateProfile(update: ProfileUpdate): EditableProfile
    suspend fun uploadImage(kind: ProfileImageKind, bytes: ByteArray): EditableProfile
    suspend fun deleteImage(kind: ProfileImageKind): EditableProfile
    suspend fun changePassword(oldPassword: String, newPassword: String)
    suspend fun requestPasswordReset(email: String, captchaResponse: String? = null)
}
