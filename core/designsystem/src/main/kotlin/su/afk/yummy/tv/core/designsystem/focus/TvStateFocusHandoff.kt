package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import su.afk.yummy.tv.core.designsystem.locals.LocalPreferredContentFocusRequester

/**
 * Передача фокуса «лоадер/ошибка → контент» на ТВ-экранах.
 *
 * Пока экран грузится, в нём нет фокусируемых элементов, и фокус падает в боковое меню
 * (оно раскрывается), а стартовая попытка скаффолда посадить фокус в контент сгорает по
 * таймауту. Плейсхолдер (лоадер или «Повторить») держит фокус и регистрируется как
 * preferred-фокус контента; когда он уходит из композиции, фокус забирает контент.
 */
@Stable
class TvStateFocusHandoff internal constructor() {
    /** Вешается на лоадер и на кнопку «Повторить» — одновременно на экране они не бывают. */
    val placeholderFocusRequester = FocusRequester()
    internal var holdsFocus by mutableStateOf(false)
    internal var placeholder by mutableStateOf<TvStatePlaceholder?>(null)

    /** Какой плейсхолдер последним получил фокус — чтобы отличить уход пользователя от смены плейсхолдера. */
    internal var focusedPlaceholder: TvStatePlaceholder? = null

    /** Фокус был на плейсхолдере, плейсхолдер сменился контентом — контент должен забрать фокус. */
    val shouldFocusContent: Boolean
        get() = holdsFocus && placeholder == null

    /** Контент забрал фокус (или сам экран перенёс его). */
    fun onContentFocused() {
        holdsFocus = false
    }
}

/**
 * @param placeholder что показано вместо контента; `null` — показан контент.
 * @param contentFocusRequester куда перенести фокус после загрузки. Если `null`, перенос
 * делает сам экран по [TvStateFocusHandoff.shouldFocusContent] и вызывает
 * [TvStateFocusHandoff.onContentFocused].
 */
@Composable
fun rememberTvStateFocusHandoff(
    placeholder: TvStatePlaceholder?,
    contentFocusRequester: FocusRequester? = null,
): TvStateFocusHandoff {
    val handoff = remember { TvStateFocusHandoff() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current

    // Пишем прямо в композиции (как rememberUpdatedState), а не в SideEffect: при диспозе
    // плейсхолдера onFocusChanged срабатывает раньше SideEffect и увидел бы старое значение.
    handoff.placeholder = placeholder

    if (placeholder != null) {
        DisposableEffect(registerPreferredContentFocusRequester) {
            registerPreferredContentFocusRequester?.invoke(handoff.placeholderFocusRequester)
            onDispose { registerPreferredContentFocusRequester?.invoke(null) }
        }
    }

    // Лоадер → ошибка → «Повторить» → лоадер: узел с фокусом уходит из композиции,
    // фокус переводим на новый плейсхолдер, чтобы он не упал в меню.
    LaunchedEffect(placeholder, handoff.holdsFocus) {
        if (placeholder != null && handoff.holdsFocus) {
            requestFocusUntilTimeout(handoff.placeholderFocusRequester)
        }
    }

    if (contentFocusRequester != null) {
        LaunchedEffect(handoff.shouldFocusContent, contentFocusRequester) {
            if (handoff.shouldFocusContent) {
                requestFocusUntilTimeout(contentFocusRequester)
                handoff.onContentFocused()
            }
        }
    }

    return handoff
}

/**
 * Отслеживает, что фокус внутри плейсхолдера [kind]. Потеря фокуса, пока этот плейсхолдер
 * ещё на экране, — пользователь ушёл сам (например в меню), тогда фокус у него не отбираем.
 * Потеря из-за смены плейсхолдера (лоадер → ошибка) или появления контента флаг не сбрасывает.
 *
 * Сам перенос фокуса при удалении сфокусированного плейсхолдера обеспечивает ТВ-скаффолд:
 * Android возвращает фокус в Compose «на вход», и скаффолд направляет его в контент, а не в меню.
 */
fun Modifier.tvStateFocusTracking(
    handoff: TvStateFocusHandoff,
    kind: TvStatePlaceholder,
): Modifier =
    onFocusChanged {
        if (it.hasFocus) {
            handoff.focusedPlaceholder = kind
            handoff.holdsFocus = true
        } else if (handoff.focusedPlaceholder == kind && handoff.placeholder == kind) {
            // Только настоящая потеря фокуса этим же плейсхолдером. Стартовое Inactive
            // у только что появившейся ошибки сюда не попадает.
            handoff.holdsFocus = false
        }
    }

/** Фокусируемый лоадер, который держит фокус до появления контента. */
fun Modifier.tvFocusablePlaceholder(handoff: TvStateFocusHandoff): Modifier =
    focusRequester(handoff.placeholderFocusRequester)
        .tvStateFocusTracking(handoff, TvStatePlaceholder.Loading)
        .focusable()
