package su.afk.yummy.tv.domain.account.model

data class ProfileUpdate(
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
