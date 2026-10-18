package su.afk.yummy.tv.feature.account.account.model

import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.feature.account.common.model.ProfileStatSlice

internal data class ProfileStatsPageModel(
    val title: String,
    val icon: ImageVector,
    val slices: List<ProfileStatSlice>,
    val valueType: ProfileStatsValueType,
    val legendColumns: Int = 2,
    val chartType: ProfileStatsChartType = ProfileStatsChartType.DONUT,
)
