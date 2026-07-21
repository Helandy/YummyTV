package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.domain.account.repository.ProfileSettingsRepository
import javax.inject.Inject

/** Загружает изображение выбранного типа для профиля текущего пользователя. */
class UploadProfileImageUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke(kind: ProfileImageKind, bytes: ByteArray) =
        repository.uploadImage(kind, bytes)
}
