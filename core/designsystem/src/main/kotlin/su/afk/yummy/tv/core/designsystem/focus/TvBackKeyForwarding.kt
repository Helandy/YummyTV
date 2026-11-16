package su.afk.yummy.tv.core.designsystem.focus

import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView

/**
 * Передаёт не обработанный никем BACK в `OnBackPressedDispatcher` своего окна — туда, где его ждут
 * `BackHandler` и `onDismissRequest` диалога. Вешать на корень контента окна: `TvMainScaffold` и
 * каждый ТВ-`Dialog` (у `AlertDialog` — через `modifier`). Диспетчер берётся по месту модификатора,
 * поэтому внутри диалога это диспетчер самого диалога.
 *
 * Без этого на Android ≤ 15 (predictive back у нас не включён) Compose превращает необработанный
 * BACK в `FocusDirection.Exit`: внутри групп фокуса «выход» съедает нажатие, система не видит
 * отслеженного KeyDown, и первый «Назад» только снимает фокус — диалог закрывается со второго.
 *
 * На Android 16+ KeyUp система сама отдаёт в OnBackInvokedCallback, а во view-дерево он приходит
 * уже отменённым — его пропускаем, поэтому срабатывание остаётся одним.
 *
 * Это `onKeyEvent`-узел: дочерние элементы, которые сами обрабатывают BACK (поля ввода), срабатывают раньше.
 */
fun Modifier.tvBackKeyForwarding(): Modifier = this then TvBackKeyForwardingElement

private data object TvBackKeyForwardingElement : ModifierNodeElement<TvBackKeyForwardingNode>() {
    override fun create() = TvBackKeyForwardingNode()

    override fun update(node: TvBackKeyForwardingNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "tvBackKeyForwarding"
    }
}

private class TvBackKeyForwardingNode :
    Modifier.Node(),
    KeyInputModifierNode,
    CompositionLocalConsumerModifierNode {
    // DOWN этого BACK не обработал ни один дочерний элемент — значит, UP тоже наш.
    private var unhandledDown = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.key != Key.Back) return false
        return when (event.type) {
            KeyEventType.KeyDown -> {
                if (event.nativeKeyEvent.repeatCount == 0) unhandledDown = true
                true
            }

            KeyEventType.KeyUp -> {
                if (!unhandledDown) return false
                unhandledDown = false
                if (!event.nativeKeyEvent.isCanceled) {
                    currentValueOf(LocalView).findViewTreeOnBackPressedDispatcherOwner()
                        ?.onBackPressedDispatcher
                        ?.onBackPressed()
                }
                true
            }

            else -> false
        }
    }

    override fun onPreKeyEvent(event: KeyEvent): Boolean = false
}
