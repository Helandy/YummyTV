package su.afk.yummy.tv.feature.account.profileedit.model

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.domain.account.model.ProfileListPrivacy
import su.afk.yummy.tv.domain.account.model.UserProfileSex

/** Редактируемые поля формы профиля, уходящие на сервер одним сохранением. */
@Immutable
data class ProfileForm(
    val about: String,
    val birthDate: String,
    val sex: UserProfileSex,
    val listPrivacy: ProfileListPrivacy,
    val showShikimori: Boolean,
    val showTelegram: Boolean,
    val showVk: Boolean,
    val showDiscord: Boolean,
    val notifyTelegram: Boolean,
    val notifyVk: Boolean,
)
