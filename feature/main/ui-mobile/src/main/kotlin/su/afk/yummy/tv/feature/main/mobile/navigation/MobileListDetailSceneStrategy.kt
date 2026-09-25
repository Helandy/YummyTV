package su.afk.yummy.tv.feature.main.mobile.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategy
import su.afk.yummy.tv.core.designsystem.mobile.layout.MobileCompactListPane
import su.afk.yummy.tv.core.navigation.scene.CompactListPaneDestination
import su.afk.yummy.tv.core.navigation.scene.RequireListPaneSceneStrategy

/**
 * List-detail раскладка мобильного графа (посты, диалоги, рецензии) на широком окне:
 * - «назад» снимает одну запись ([BackNavigationBehavior.PopLatest]). По умолчанию записи
 *   снимаются, пока не сменится раскладка, а при заглушке справа она не меняется, и вместе
 *   с деталью уходил список (вплоть до выхода из приложения);
 * - зазор между панелями 1dp вместо 24dp: широкий зазор показывает фон и режет верхние бары
 *   панелей (они цвета surface и заходят под статус-бар), а 1dp остаётся тонким разделителем;
 * - пока наверху [CompactListPaneDestination] (чат), панель списка сужается до колонки аватарок;
 * - сцена строится только при списке в стеке ([RequireListPaneSceneStrategy]).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberMobileListDetailSceneStrategy(
    currentDestination: NavKey?,
): SceneStrategy<NavKey> {
    val listPaneExpansion = rememberPaneExpansionState()
    val compactListPane = currentDestination is CompactListPaneDestination
    val density = LocalDensity.current
    LaunchedEffect(compactListPane, density) {
        if (compactListPane) {
            listPaneExpansion.setFirstPaneWidth(
                with(density) { MobileCompactListPane.Width.roundToPx() },
            )
        } else {
            // Стандартная ширина списка для остальных list-detail экранов.
            listPaneExpansion.clear()
        }
    }

    val listDetailSceneStrategy = rememberListDetailSceneStrategy<NavKey>(
        backNavigationBehavior = BackNavigationBehavior.PopLatest,
        directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
            .copy(horizontalPartitionSpacerSize = ListDetailPaneDividerWidth),
        paneExpansionState = listPaneExpansion,
    )
    return RequireListPaneSceneStrategy(listDetailSceneStrategy)
}

private val ListDetailPaneDividerWidth = 1.dp
