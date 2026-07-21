package su.afk.yummy.tv.feature.details.mobile.details.model

internal data class RatingLabel(
    val label: String,
    val isPrimary: Boolean,
    val rating: Double? = null,
)
