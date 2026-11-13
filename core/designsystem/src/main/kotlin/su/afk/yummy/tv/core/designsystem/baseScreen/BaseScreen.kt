package su.afk.yummy.tv.core.designsystem.baseScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.motion.YummyMotion
import su.afk.yummy.tv.core.model.ErrorItem

internal enum class BaseScreenContentState {
    Loading,
    Error,
    Empty,
    Content,
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("OPT_IN_USAGE_IN_SIGNATURE")
@Composable
fun BaseScreen(
    contentAlignment: Alignment = Alignment.TopStart,
    contentModifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
    ),

    isScroll: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),

    topBar: (@Composable ColumnScope.() -> Unit)? = null,
    customTopBar: (@Composable (TopAppBarScrollBehavior?) -> Unit)? = null,
    topBarScroll: TopBarScroll = TopBarScroll.None,
    topBarWindowInsets: WindowInsets = TopAppBarDefaults.windowInsets,

    error: ErrorItem? = null,
    onRetry: () -> Unit = {},
    isLoading: Boolean = false,
    isEmpty: Boolean = false,

    errorContent: (@Composable (ErrorItem, onRetry: () -> Unit) -> Unit)? = null,
    loadingContent: (@Composable () -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,

    floatingActionButtonEnd: (@Composable () -> Unit)? = null,
    floatingActionButtonStart: (@Composable () -> Unit)? = null,
    floatingActionButtonBottomPadding: Dp = 0.dp,

    content: @Composable ColumnScope.() -> Unit,
) {
    key(topBarScroll) {
        val topAppBarState = rememberTopAppBarState()

        val scrollBehavior = when (topBarScroll) {
            TopBarScroll.None -> null
            TopBarScroll.EnterAlways ->
                TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

            TopBarScroll.ExitUntilCollapsed ->
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

            TopBarScroll.Pinned ->
                TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        }

        Scaffold(
            contentWindowInsets = contentWindowInsets,
            modifier = Modifier
                .fillMaxSize()
                .let {
                    if (scrollBehavior != null) {
                        it.nestedScroll(scrollBehavior.nestedScrollConnection)
                    } else {
                        it
                    }
                },
            topBar = {
                if (customTopBar != null) {
                    customTopBar(scrollBehavior)
                } else {
                    topBar?.let { slot ->
                        StandardTopBar(
                            content = { slot() },
                            scrollBehavior = scrollBehavior,
                            topBarWindowInsets = topBarWindowInsets,
                        )
                    }
                }
            },
            floatingActionButton = {
                if (floatingActionButtonStart != null || floatingActionButtonEnd != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 4.dp,
                                end = 4.dp,
                                bottom = floatingActionButtonBottomPadding,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Box { floatingActionButtonStart?.invoke() }
                        Box { floatingActionButtonEnd?.invoke() }
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
        ) { innerPadding ->
            val base = contentModifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding)

            val bodyModifier = if (isScroll) base.verticalScroll(rememberScrollState()) else base

            val screenState = when {
                error != null -> BaseScreenContentState.Error
                isLoading -> BaseScreenContentState.Loading
                isEmpty -> BaseScreenContentState.Empty
                else -> BaseScreenContentState.Content
            }

            AnimatedContent(
                targetState = screenState,
                transitionSpec = {
                    fadeIn(tween(YummyMotion.Short)) togetherWith fadeOut(tween(YummyMotion.Quick))
                },
                label = "baseScreenContent",
            ) { targetState ->
                when (targetState) {
                    BaseScreenContentState.Error -> Box(modifier = base) {
                        val currentError = error ?: return@Box
                        (
                            errorContent ?: { e, retry ->
                                DefaultErrorContent(
                                    errorItem = e,
                                    onRetry = retry,
                                )
                            }
                            )(currentError, onRetry)
                    }

                    BaseScreenContentState.Loading -> Box(modifier = base) {
                        (loadingContent ?: { DefaultLoadingContent() })()
                    }

                    BaseScreenContentState.Empty -> Box(
                        modifier = base,
                        contentAlignment = Alignment.Center,
                    ) {
                        (emptyContent ?: { DefaultEmptyContent() })()
                    }

                    BaseScreenContentState.Content -> {
                        Box(
                            modifier = bodyModifier,
                            contentAlignment = contentAlignment,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                content = content,
                            )
                        }
                    }
                }
            }
        }
    }
}
