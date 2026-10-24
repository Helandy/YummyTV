package su.afk.yummy.tv.domain.account.model

import su.afk.yummy.tv.core.model.anime.AnimePoster

/**
 * Строка списка `GET /users/{id}/lists/subs` — одна подписка на тайтл.
 *
 * [dubbing] НЕ содержит озвучку подписки: сервер возвращает либо пустую строку, либо перечисление всех
 * озвучек плеера («AniLibria() AniDUB() ...»), одинаковое для всех подписок этого балансера. Состояние
 * подписки по конкретной озвучке приходит полем `subscribed` в `/anime/{id}/videos`, а этот список нужен
 * только экрану «Мои подписки».
 */
data class VideoSubscription(
    val animeId: Int,
    val animeUrl: String,
    val playerId: Int?,
    val player: String,
    val dubbing: String,
    val poster: AnimePoster?,
    val title: String,
)
