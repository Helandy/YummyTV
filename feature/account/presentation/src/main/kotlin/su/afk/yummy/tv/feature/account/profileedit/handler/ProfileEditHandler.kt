package su.afk.yummy.tv.feature.account.profileedit.handler

import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.domain.account.model.ProfileUpdate
import su.afk.yummy.tv.domain.account.usecase.ChangePasswordUseCase
import su.afk.yummy.tv.domain.account.usecase.DeleteProfileImageUseCase
import su.afk.yummy.tv.domain.account.usecase.GetEditableProfileUseCase
import su.afk.yummy.tv.domain.account.usecase.UnlinkAccountUseCase
import su.afk.yummy.tv.domain.account.usecase.UpdateProfileUseCase
import su.afk.yummy.tv.domain.account.usecase.UploadProfileImageUseCase
import javax.inject.Inject

class ProfileEditHandler @Inject constructor(
    private val getProfile: GetEditableProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val uploadImage: UploadProfileImageUseCase,
    private val deleteImage: DeleteProfileImageUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val unlinkAccountUseCase: UnlinkAccountUseCase,
) {
    suspend fun load() = getProfile()
    suspend fun update(update: ProfileUpdate) = updateProfile(update)
    suspend fun upload(kind: ProfileImageKind, bytes: ByteArray) = uploadImage(kind, bytes)
    suspend fun delete(kind: ProfileImageKind) = deleteImage(kind)
    suspend fun changePassword(oldPassword: String, newPassword: String) =
        changePasswordUseCase(oldPassword, newPassword)

    suspend fun unlinkAccount(provider: LinkedAccountProvider) = unlinkAccountUseCase(provider)
}
