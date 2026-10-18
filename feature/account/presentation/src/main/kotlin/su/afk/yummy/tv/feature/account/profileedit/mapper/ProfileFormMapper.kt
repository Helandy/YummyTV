package su.afk.yummy.tv.feature.account.profileedit.mapper

import su.afk.yummy.tv.feature.account.profileedit.ProfileEditState
import su.afk.yummy.tv.feature.account.profileedit.model.ProfileForm

/** Снимок редактируемых полей; нормализация совпадает с тем, что уходит на сервер. */
fun ProfileEditState.State.toForm() = ProfileForm(
    about = about.trim(),
    birthDate = birthDate,
    sex = sex,
    listPrivacy = listPrivacy,
    showShikimori = showShikimori,
    showTelegram = showTelegram,
    showVk = showVk,
    showDiscord = showDiscord,
    notifyTelegram = notifyTelegram,
    notifyVk = notifyVk,
)
