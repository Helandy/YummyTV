package su.afk.yummy.tv.feature.main.mobile.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.main.mobile.R

/** Вкладки мобильной навигации (бар или рейка) в порядке показа. */
@Composable
internal fun mobileMenuItems(unreadNotificationsCount: Int): List<MobileMenuItem<RootTab>> = listOf(
    MobileMenuItem(
        stringResource(R.string.main_mobile_tab_news),
        RootTab.POSTS,
        Icons.Default.Newspaper,
    ),
    MobileMenuItem(
        stringResource(R.string.main_mobile_tab_top),
        RootTab.TOP,
        Icons.Default.Star,
    ),
    MobileMenuItem(
        stringResource(R.string.main_mobile_tab_home),
        RootTab.HOME,
        Icons.Default.Home,
    ),
    MobileMenuItem(
        stringResource(R.string.main_mobile_tab_library),
        RootTab.LIBRARY,
        Icons.AutoMirrored.Filled.List,
    ),
    MobileMenuItem(
        stringResource(R.string.main_mobile_tab_profile),
        RootTab.ACCOUNT,
        Icons.Default.AccountCircle,
        badgeCount = unreadNotificationsCount,
    ),
)
