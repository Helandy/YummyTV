package su.afk.yummy.tv.data.account.mapper

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.account.dto.YaniProfileDto
import su.afk.yummy.tv.data.account.dto.YaniUserProfileDto
import su.afk.yummy.tv.domain.account.model.EditableProfile
import su.afk.yummy.tv.domain.account.model.FriendshipStatus
import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.domain.account.model.ProfileListPrivacy
import su.afk.yummy.tv.domain.account.model.UserProfileSex
import su.afk.yummy.tv.domain.account.model.UserSearchItem

internal fun YaniUserProfileDto.toUserSearchItem(): UserSearchItem = UserSearchItem(
    id = id,
    nickname = nickname,
    avatarUrl = avatars?.full?.toHttpsUrl() ?: avatars?.big?.toHttpsUrl()
    ?: avatars?.small?.toHttpsUrl(),
    lastOnlineSeconds = lastOnline ?: 0L,
)

internal fun YaniProfileDto.toEditableProfile(): EditableProfile = EditableProfile(
    userId = id,
    nickname = nickname,
    avatarUrl = avatars?.full?.toHttpsUrl() ?: avatars?.big?.toHttpsUrl()
    ?: avatars?.small?.toHttpsUrl(),
    bannerUrl = banner?.full?.toHttpsUrl() ?: banner?.cropped?.toHttpsUrl(),
    about = about,
    birthDateSeconds = birthDate ?: 0L,
    sex = sex.toProfileSex(),
    listPrivacy = listsPrivacy.toProfileListPrivacy(),
    showShikimori = privacy?.shikimoriPublic ?: true,
    showTelegram = privacy?.telegramPublic ?: true,
    showVk = privacy?.vkPublic ?: true,
    showDiscord = privacy?.discordPublic ?: true,
    notifyTelegram = notifications?.telegram ?: false,
    notifyVk = notifications?.vk ?: false,
    linkedAccounts = buildSet {
        if (ids?.vk.isLinkedValue()) add(LinkedAccountProvider.VK)
        if (!ids?.telegramNickname.isNullOrBlank()) add(LinkedAccountProvider.TELEGRAM)
        if (ids?.discord.isLinkedValue()) add(LinkedAccountProvider.DISCORD)
        if (ids?.shikimori.isLinkedValue()) add(LinkedAccountProvider.SHIKIMORI)
    },
)

private fun kotlinx.serialization.json.JsonElement?.isLinkedValue(): Boolean = when (this) {
    null, JsonNull -> false
    is JsonObject -> isNotEmpty()
    is JsonArray -> isNotEmpty()
    is JsonPrimitive -> content.isNotBlank() && content !in setOf("0", "false", "null")
}

internal fun String?.toFriendshipStatus(): FriendshipStatus = when (this) {
    "friends" -> FriendshipStatus.FRIENDS
    "followers" -> FriendshipStatus.FOLLOWERS
    "following" -> FriendshipStatus.FOLLOWING
    "requests" -> FriendshipStatus.REQUESTS
    "sent-requests" -> FriendshipStatus.SENT_REQUESTS
    else -> FriendshipStatus.NONE
}

private fun Int?.toProfileSex(): UserProfileSex = when (this) {
    1 -> UserProfileSex.MALE
    2 -> UserProfileSex.FEMALE
    else -> UserProfileSex.UNKNOWN
}

private fun String?.toProfileListPrivacy(): ProfileListPrivacy = when (this) {
    "friends" -> ProfileListPrivacy.FRIENDS
    "authed" -> ProfileListPrivacy.AUTHORIZED
    "none" -> ProfileListPrivacy.PRIVATE
    else -> ProfileListPrivacy.PUBLIC
}
