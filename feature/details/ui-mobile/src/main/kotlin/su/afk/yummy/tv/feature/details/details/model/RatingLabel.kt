package su.afk.yummy.tv.feature.details.details.model

internal data class RatingLabel(
    val label: String,
    val isPrimary: Boolean,
    val rating: Double? = null,
)
