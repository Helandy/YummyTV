package su.afk.yummy.tv.domain.collection.model

/** Порядок публичных коллекций в каталоге; [apiValue] уходит в параметр `sort`. */
enum class CollectionSort(val apiValue: String) {
    NEW("new"),
    BEST("best"),
    OLD("old"),
}
