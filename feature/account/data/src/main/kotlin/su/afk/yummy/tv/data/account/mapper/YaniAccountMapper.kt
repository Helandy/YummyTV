package su.afk.yummy.tv.data.account.mapper

import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.account.dto.YaniProfileDto
import su.afk.yummy.tv.domain.account.model.YaniAccount

internal fun YaniProfileDto.toAccount(): YaniAccount =
    YaniAccount(
        id = id,
        nickname = nickname,
        avatarUrl = avatars?.full?.toHttpsUrl() ?: avatars?.big?.toHttpsUrl()
        ?: avatars?.small?.toHttpsUrl(),
    )
