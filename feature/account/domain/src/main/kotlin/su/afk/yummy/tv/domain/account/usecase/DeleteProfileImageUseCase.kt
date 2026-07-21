package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.domain.account.repository.ProfileSettingsRepository
import javax.inject.Inject

/** Удаляет изображение выбранного типа из профиля текущего пользователя. */
class DeleteProfileImageUseCase @Inject constructor(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke(kind: ProfileImageKind) = repository.deleteImage(kind)
}
