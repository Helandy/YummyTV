package su.afk.yummy.tv.core.designsystem.mobile.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * `true`, если экран сейчас нарисован панелью list-detail сцены рядом со списком. Стратегия
 * берёт сцену только при двух видимых панелях, поэтому на телефоне и при открытии детали
 * без списка под ней (диплинк, переход из другой фичи) здесь `false`. Детали прячут по нему
 * стрелку «назад»: список и так виден слева.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
@ReadOnlyComposable
fun isInListDetailPane(): Boolean = LocalListDetailSceneScope.current != null
