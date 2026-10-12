package su.afk.yummy.tv.feature.account.mobile.account.model

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.PersistentList
import su.afk.yummy.tv.feature.account.common.model.ProfileStatSlice

internal data class AccountMobileProfileStatsPageModel(
    val title: String,
    val icon: ImageVector,
    val slices: PersistentList<ProfileStatSlice>,
    val valueType: AccountMobileProfileStatsValueType,
    val legendColumns: Int = 2,
)
