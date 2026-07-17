package su.afk.yummy.tv.domain.account.model

data class EditableProfile(
    val userId: Int,
    val nickname: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val about: String,
    val birthDateSeconds: Long,
    val sex: UserProfileSex,
    val listPrivacy: ProfileListPrivacy,
    val showShikimori: Boolean,
    val showTelegram: Boolean,
    val showVk: Boolean,
    val showDiscord: Boolean,
    val notifyTelegram: Boolean,
    val notifyVk: Boolean,
    val linkedAccounts: Set<LinkedAccountProvider>,
)

enum class LinkedAccountProvider(val apiValue: String) {
    VK("vk"),
    TELEGRAM("tg"),
    DISCORD("discord"),
    SHIKIMORI("shiki"),
}

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

enum class ProfileListPrivacy {
    PUBLIC,
    FRIENDS,
    AUTHORIZED,
    PRIVATE,
}

enum class ProfileImageKind {
    AVATAR,
    BANNER,
}
