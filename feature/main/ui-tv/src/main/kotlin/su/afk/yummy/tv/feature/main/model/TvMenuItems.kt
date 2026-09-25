package su.afk.yummy.tv.feature.main.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.main.R

/** Вкладки бокового меню TV в порядке показа (аккаунт рисуется отдельно, внизу меню). */
internal val tvMenuItems: List<TvMenuItem> = listOf(
    TvMenuItem(R.string.main_tab_search, RootTab.SEARCH, Icons.Default.Search),
    TvMenuItem(R.string.main_tab_schedule, RootTab.SCHEDULE, Icons.Default.CalendarMonth),
    TvMenuItem(R.string.main_tab_home, RootTab.HOME, Icons.Default.Home),
    TvMenuItem(R.string.main_tab_collections, RootTab.COLLECTIONS, Icons.Filled.CollectionsBookmark),
    TvMenuItem(R.string.main_tab_news, RootTab.POSTS, Icons.Default.Newspaper),
    TvMenuItem(R.string.main_tab_top, RootTab.TOP, Icons.Default.Star),
    TvMenuItem(R.string.main_tab_library, RootTab.LIBRARY, Icons.AutoMirrored.Filled.List),
)
