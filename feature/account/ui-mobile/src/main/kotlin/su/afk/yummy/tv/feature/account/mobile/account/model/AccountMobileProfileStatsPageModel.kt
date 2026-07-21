package su.afk.yummy.tv.feature.account.mobile.account.model

internal data class AccountMobileProfileStatsPageModel(
    val title: String,
    val slices: List<AccountMobileProfileStatSlice>,
    val valueType: AccountMobileProfileStatsValueType,
    val compactLegend: Boolean = false,
)
