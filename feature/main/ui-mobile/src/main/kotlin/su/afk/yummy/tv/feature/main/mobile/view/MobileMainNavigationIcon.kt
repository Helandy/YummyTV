package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem

@Composable
internal fun <T> MobileMainNavigationIcon(item: MobileMenuItem<T>) {
    BadgedBox(
        badge = {
            if (item.badgeCount > 0) {
                Badge { Text(item.badgeCount.toString()) }
            }
        },
    ) {
        Icon(item.icon, contentDescription = item.label)
    }
}
