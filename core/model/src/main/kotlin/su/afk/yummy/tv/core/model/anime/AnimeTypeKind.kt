package su.afk.yummy.tv.core.model.anime

/** Тип тайтла Yani, не зависящий от языка: от него зависит значок ноды на таймлайне франшизы. */
enum class AnimeTypeKind {
    TV,
    SHORT_TV,
    MOVIE,
    SHORT_MOVIE,
    OVA,
    ONA,
    SPECIAL,

    /** Тип, которого клиент пока не знает. */
    OTHER,
}
