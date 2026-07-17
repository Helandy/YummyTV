package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.domain.account.model.ProfileUpdate
import su.afk.yummy.tv.domain.account.repository.ProfileSettingsRepository
import javax.inject.Inject

class GetEditableProfileUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke() = repository.getProfile()
}

class UpdateProfileUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke(update: ProfileUpdate) = repository.updateProfile(update)
}

class UploadProfileImageUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke(kind: ProfileImageKind, bytes: ByteArray) =
        repository.uploadImage(kind, bytes)
}

class DeleteProfileImageUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke(kind: ProfileImageKind) = repository.deleteImage(kind)
}

class ChangePasswordUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke(oldPassword: String, newPassword: String) =
        repository.changePassword(oldPassword, newPassword)
}

class RequestPasswordResetUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke(email: String, captchaResponse: String? = null) =
        repository.requestPasswordReset(email, captchaResponse)
}
