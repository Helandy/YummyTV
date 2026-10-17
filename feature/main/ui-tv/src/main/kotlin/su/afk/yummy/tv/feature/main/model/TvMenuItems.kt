package su.afk.yummy.tv.feature.main.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.main.presentation.R

/** Вкладки бокового меню TV в порядке показа (аккаунт рисуется отдельно, внизу меню). */
internal val tvMenuItems: List<TvMenuItem> = listOf(
    TvMenuItem(R.string.main_tab_schedule, RootTab.SCHEDULE, Icons.Default.CalendarMonth),
    TvMenuItem(R.string.main_tab_catalog, RootTab.CATALOG, Icons.Default.GridView),
    TvMenuItem(R.string.main_tab_home, RootTab.HOME, Icons.Default.Home),
    TvMenuItem(R.string.main_tab_collections, RootTab.COLLECTIONS, Icons.Filled.CollectionsBookmark),
    TvMenuItem(R.string.main_tab_news, RootTab.POSTS, Icons.Default.Newspaper),
    TvMenuItem(R.string.main_tab_library, RootTab.LIBRARY, Icons.AutoMirrored.Filled.List),
    TvMenuItem(R.string.main_tab_history, RootTab.HISTORY, Icons.Default.History),
)
