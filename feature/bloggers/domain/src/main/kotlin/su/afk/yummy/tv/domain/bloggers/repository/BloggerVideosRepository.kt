package su.afk.yummy.tv.domain.bloggers.repository

import su.afk.yummy.tv.domain.bloggers.model.BloggerDetails
import su.afk.yummy.tv.domain.bloggers.model.BloggerDirectory
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoReaction
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote

interface BloggerVideosRepository {
    suspend fun getVideos(
        category: String = "all",
        bloggerId: Int? = null,
        sort: BloggerVideoSort = BloggerVideoSort.NEW,
        limit: Int = 20,
        offset: Int = 0,
    ): List<BloggerVideo>

    suspend fun getAnimeVideos(animeId: Int, limit: Int = 24, offset: Int = 0): List<BloggerVideo>
    suspend fun getDirectory(limit: Int = 40): BloggerDirectory
    suspend fun getBlogger(id: Int): BloggerDetails
    suspend fun getVideo(id: Int): BloggerVideo
    suspend fun setSubscribed(id: Int, subscribed: Boolean): Int
    suspend fun setVideoVote(id: Int, vote: BloggerVideoVote): BloggerVideoReaction
}
