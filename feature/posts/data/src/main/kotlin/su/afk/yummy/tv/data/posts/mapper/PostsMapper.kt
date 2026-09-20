package su.afk.yummy.tv.data.posts.mapper

import su.afk.yummy.tv.core.utils.network.toHttpsUrl
import su.afk.yummy.tv.data.posts.dto.YaniPostAuthorDto
import su.afk.yummy.tv.data.posts.dto.YaniPostCategoryDto
import su.afk.yummy.tv.data.posts.dto.YaniPostDetailsDto
import su.afk.yummy.tv.data.posts.dto.YaniPostSummaryDto
import su.afk.yummy.tv.data.posts.dto.YaniPostVoteResultDto
import su.afk.yummy.tv.domain.posts.model.PostAuthor
import su.afk.yummy.tv.domain.posts.model.PostCategory
import su.afk.yummy.tv.domain.posts.model.PostDetails
import su.afk.yummy.tv.domain.posts.model.PostReaction
import su.afk.yummy.tv.domain.posts.model.PostSummary
import su.afk.yummy.tv.domain.posts.model.PostVote
import su.afk.yummy.tv.domain.posts.model.RelatedPostAnime

internal fun YaniPostCategoryDto.domain() = PostCategory(id, title, uri)

internal fun YaniPostAuthorDto.domain() = PostAuthor(
    id,
    nickname,
    (avatars?.full ?: avatars?.big ?: avatars?.small)?.toHttpsUrl(),
)

internal fun YaniPostSummaryDto.domain() = PostSummary(
    id,
    title,
    previewImage?.toHttpsUrl(),
    contentPreview,
    user.domain(),
    category.domain(),
    createdAt,
)

internal fun YaniPostVoteResultDto.domain(vote: PostVote) = PostReaction(likes, dislikes, vote)

internal fun YaniPostDetailsDto.domain() = PostDetails(
    id = id,
    title = title,
    contentHtml = content,
    previewImageUrl = previewImage?.toHttpsUrl(),
    author = user.domain(),
    category = category.domain(),
    createdAt = createdAt,
    editedAt = editedAt,
    relatedAnime = animes.distinctBy { it.animeId }.map {
        RelatedPostAnime(
            it.animeId,
            it.title,
            it.poster?.run { mega ?: huge ?: big ?: medium ?: small }?.toHttpsUrl(),
            it.year,
            it.rating?.average,
        )
    },
    reaction = PostReaction(
        likes.likes,
        likes.dislikes,
        PostVote.entries.firstOrNull { it.action == likes.vote } ?: PostVote.NONE),
    views = views,
    comments = comments,
)
