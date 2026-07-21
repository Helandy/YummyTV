package su.afk.yummy.tv.feature.account.account.model

internal data class ProfileStatsPageModel(
    val title: String,
    val slices: List<ProfileStatSlice>,
    val valueType: ProfileStatsValueType,
    val compactLegend: Boolean = false,
)
