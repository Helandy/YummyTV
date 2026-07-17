package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.FriendshipStatus
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserSearchItem

interface UserDirectoryRepository {
    suspend fun search(query: String, limit: Int, offset: Int): List<UserSearchItem>
    suspend fun getProfileByNickname(nickname: String): UserProfileSummary
    suspend fun getFriendship(userId: Int, friendId: Int): FriendshipStatus
    suspend fun addFriend(userId: Int, friendId: Int)
    suspend fun removeFriend(userId: Int, friendId: Int)
}
