package su.afk.yummy.tv.domain.library.model

/** Поведение импорта, когда тайтл уже находится в списке пользователя. */
enum class ImportConflictPolicy {
    /** Не менять тайтлы, у которых уже выставлен статус списка. */
    SKIP_EXISTING,

    /** Обновить статус списка и (если включён перенос оценок) оценку из файла. */
    OVERWRITE,
}
