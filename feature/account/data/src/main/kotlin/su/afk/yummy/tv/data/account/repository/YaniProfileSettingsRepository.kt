package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.data.account.dto.YaniProfileHideBodyDto
import su.afk.yummy.tv.data.account.dto.YaniProfileNotificationsBodyDto
import su.afk.yummy.tv.data.account.dto.YaniProfileUpdateBodyDto
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.network.YaniCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.EditableProfile
import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.domain.account.model.ProfileListPrivacy
import su.afk.yummy.tv.domain.account.model.ProfileUpdate
import su.afk.yummy.tv.domain.account.model.UserProfileSex
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.repository.ProfileSettingsRepository

class YaniProfileSettingsRepository(
    private val api: YaniAccountApi,
    private val accountRepository: AccountRepository,
    private val authPreferences: YaniAuthPreferences,
) : ProfileSettingsRepository {
    override suspend fun getProfile(): EditableProfile = withContext(Dispatchers.IO) {
        accountRepository.refreshProfile()
    }

    override suspend fun updateProfile(update: ProfileUpdate): EditableProfile =
        withContext(Dispatchers.IO) {
            api.updateProfile(
                YaniProfileUpdateBodyDto(
                    about = update.about,
                    birthDate = update.birthDate,
                    sex = update.sex.apiValue(),
                    listsPrivacy = update.listPrivacy.apiValue(),
                    hide = YaniProfileHideBodyDto(
                        shiki = !update.showShikimori,
                        tg = !update.showTelegram,
                        vk = !update.showVk,
                        discord = !update.showDiscord,
                    ),
                    notifications = YaniProfileNotificationsBodyDto(
                        tg = update.notifyTelegram,
                        vk = update.notifyVk,
                    ),
                )
            )
            accountRepository.refreshProfile()
        }

    override suspend fun uploadImage(kind: ProfileImageKind, bytes: ByteArray): EditableProfile =
        withContext(Dispatchers.IO) {
            val profile = accountRepository.refreshProfile()
            when (kind) {
                ProfileImageKind.AVATAR -> api.uploadAvatar(profile.userId, bytes)
                ProfileImageKind.BANNER -> api.uploadBanner(profile.userId, bytes)
            }
            accountRepository.refreshProfile()
        }

    override suspend fun deleteImage(kind: ProfileImageKind): EditableProfile =
        withContext(Dispatchers.IO) {
            val profile = accountRepository.refreshProfile()
            when (kind) {
                ProfileImageKind.AVATAR -> api.deleteAvatar(profile.userId)
                ProfileImageKind.BANNER -> api.deleteBanner(profile.userId)
            }
            accountRepository.refreshProfile()
        }

    override suspend fun changePassword(oldPassword: String, newPassword: String) =
        withContext(Dispatchers.IO) {
            authPreferences.setRefreshToken(api.changePassword(oldPassword, newPassword))
        }

    override suspend fun requestPasswordReset(email: String, captchaResponse: String?) =
        withContext(Dispatchers.IO) {
            try {
                api.requestPasswordReset(email, captchaResponse)
            } catch (_: YaniCaptchaRequiredException) {
                throw AccountCaptchaRequiredException()
            }
        }

    private fun UserProfileSex.apiValue() = when (this) {
        UserProfileSex.MALE -> 1
        UserProfileSex.FEMALE -> 2
        UserProfileSex.UNKNOWN -> 0
    }

    private fun ProfileListPrivacy.apiValue() = when (this) {
        ProfileListPrivacy.PUBLIC -> "public"
        ProfileListPrivacy.FRIENDS -> "friends"
        ProfileListPrivacy.AUTHORIZED -> "authed"
        ProfileListPrivacy.PRIVATE -> "none"
    }
}
