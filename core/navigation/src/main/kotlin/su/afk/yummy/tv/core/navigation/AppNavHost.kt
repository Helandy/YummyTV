package su.afk.yummy.tv.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import su.afk.yummy.tv.core.navigation.root.RootTab

@Composable
fun AppNavHost(
    navManager: NavigationManager,
    registrars: Set<@JvmSuppressWildcards NavRegistrar>,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
        defaultTransitionSpec(),
    popTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
        defaultPopTransitionSpec(),
) {
    var savedCurrentRoot by rememberSaveable { mutableStateOf(navManager.currentRoot) }

    LaunchedEffect(Unit) {
        navManager.restoreRoot(savedCurrentRoot)
    }
    LaunchedEffect(navManager.currentRoot) {
        savedCurrentRoot = navManager.currentRoot
    }

    val appBackStack = key("appBackStack") {
        rememberNavBackStack()
    }
    val rootBackStacks = RootTab.entries.associateWith { root ->
        key(root) {
            rememberNavBackStack(navManager.roots.getValue(root))
        }
    }

    LaunchedEffect(appBackStack, rootBackStacks) {
        navManager.attachBackStacks(
            appBackStack = appBackStack,
            rootStacks = rootBackStacks,
        )
    }

    val provider = remember(registrars, navManager) {
        entryProvider<NavKey> {
            registrars.forEach { it.register(this, navManager) }
        }
    }

    val rootEntries = RootTab.entries.associateWith { root ->
        key(root) {
            rememberDecoratedNavEntries(
                backStack = rootBackStacks.getValue(root),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = provider,
            )
        }
    }

    val appEntries = key("appEntries") {
        rememberDecoratedNavEntries(
            backStack = appBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = provider,
        )
    }

    val entriesToShow: List<NavEntry<NavKey>> =
        if (appBackStack.isNotEmpty()) {
            appEntries
        } else {
            rootEntries.getValue(navManager.currentRoot)
        }

    NavDisplay(
        entries = entriesToShow,
        sceneStrategies = listOf(BottomOverlaySceneStrategy()),
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        onBack = { navManager.back() },
        modifier = modifier,
    )
}
