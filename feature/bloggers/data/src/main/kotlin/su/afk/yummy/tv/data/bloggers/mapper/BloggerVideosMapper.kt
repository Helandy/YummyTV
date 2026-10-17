package su.afk.yummy.tv.data.bloggers.mapper

import su.afk.yummy.tv.core.utils.network.toHttpsUrl
import su.afk.yummy.tv.data.bloggers.dto.BloggerDetailsDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoCategoryDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoReactionDto
import su.afk.yummy.tv.domain.bloggers.model.Blogger
import su.afk.yummy.tv.domain.bloggers.model.BloggerDetails
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoCategory
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoReaction
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote

internal fun BloggerVideoDto.toDomain() = BloggerVideo(
    id = id,
    title = title,
    description = descriptions.small.ifBlank { descriptions.big },
    previewUrl = (previews.big ?: previews.small)?.toHttpsUrl(),
    iframeUrl = iframeUrl.toHttpsUrl(),
    publishedAt = publishDate,
    views = views,
    hasSpoiler = hasSpoiler,
    category = category.toDomain(),
    creator = creator.toDomain(),
    reaction = likes.toDomain(),
    commentsCount = commentsCount,
)

internal fun BloggerVideoCategoryDto.toDomain() = BloggerVideoCategory(id, title.replaceFirstChar { it.uppercase() })

internal fun BloggerDto.toDomain() =
    Blogger(id, nickname, (avatars.big ?: avatars.small)?.toHttpsUrl())

internal fun BloggerDetailsDto.toDomain() = BloggerDetails(
    id = id,
    nickname = nickname,
    avatarUrl = (avatars.full ?: avatars.big ?: avatars.small)?.toHttpsUrl(),
    subscribers = subscriptions,
    videosCount = videosCount,
    isSubscribed = isSubscribed,
    categories = categories.map { it.toDomain() },
)

internal fun BloggerVideoReactionDto.toDomain() = BloggerVideoReaction(
    likes = likes,
    dislikes = dislikes,
    vote = when (vote) {
        1 -> BloggerVideoVote.LIKE
        -1 -> BloggerVideoVote.DISLIKE
        else -> BloggerVideoVote.NONE
    },
)
