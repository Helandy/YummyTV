package su.afk.yummy.tv.core.designsystem.presenter.baseScreen

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
sealed interface TopBarScroll {
    data object None : TopBarScroll
    data object Pinned : TopBarScroll
    data object EnterAlways : TopBarScroll
    data object ExitUntilCollapsed : TopBarScroll
}


internal enum class BaseScreenContentState {
    Loading, Error, Empty, Content,
}