package su.afk.yummy.tv.domain.home.repository

import su.afk.yummy.tv.domain.home.model.*

interface HomeFeedRepository {
    suspend fun getHomeFeed(): HomeFeed
}
