package su.afk.yummy.tv.source.anime4k.domain.model

/** Ответы сервера, которые не лечатся повтором. Остальные сбои (сеть, 5xx) приходят обычными исключениями. */
sealed class Anime4kApiException(message: String) : RuntimeException(message) {
    class RateLimited : Anime4kApiException("Anime4k: слишком много запросов")

    class Rejected : Anime4kApiException("Anime4k: тикет отклонён")

    class NotFound : Anime4kApiException("Anime4k: не найдено")
}
