package su.afk.yummy.tv.core.designsystem.motion

import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.ui.Modifier

/**
 * Анимация элемента списка при удалении и перестановке: соседи плавно съезжаются, удалённый
 * гаснет. Появление не анимируется — иначе при первой загрузке мигал бы весь список.
 *
 * Только для списков со стабильными ключами, где элементы меняет сам пользователь. В paging-сетках
 * и ТВ-списках не использовать: на refresh анимируется всё сразу, на ТВ страдает фокус.
 */
fun LazyItemScope.listItemMotion(): Modifier = Modifier.animateItem(
    fadeInSpec = null,
    placementSpec = tween(YummyMotion.Short),
    fadeOutSpec = tween(YummyMotion.Quick),
)

/** То же, что [listItemMotion], для ячеек сетки. */
fun LazyGridItemScope.gridItemMotion(): Modifier = Modifier.animateItem(
    fadeInSpec = null,
    placementSpec = tween(YummyMotion.Short),
    fadeOutSpec = tween(YummyMotion.Quick),
)
