package su.afk.yummy.tv.domain.home.model

/** Данные карточки коллекции в ленте: первые постеры и счётчики. */
data class HomeCollectionPreview(
    val posterUrls: List<String>,
    val likes: Int?,
    val views: Int?,
)
