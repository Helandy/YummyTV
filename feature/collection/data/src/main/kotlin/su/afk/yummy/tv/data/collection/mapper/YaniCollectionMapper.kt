package su.afk.yummy.tv.data.collection.mapper

import su.afk.yummy.tv.data.collection.dto.YaniCollectionVotePayloadDto
import su.afk.yummy.tv.domain.collection.model.CollectionVoteResult

internal fun YaniCollectionVotePayloadDto.toDomain(): CollectionVoteResult =
    CollectionVoteResult(
        likes = likes.coerceAtLeast(0),
        dislikes = dislikes.coerceAtLeast(0),
    )
