package su.afk.yummy.tv.data.comments.network

/** Отказ сервера с понятной пользователю причиной (например, слишком короткий комментарий). */
class YaniCommentsServerException(
    message: String,
    cause: Throwable,
) : Exception(message, cause)
