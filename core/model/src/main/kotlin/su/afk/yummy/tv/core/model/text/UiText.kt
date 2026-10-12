package su.afk.yummy.tv.core.model.text

/**
 * Текст для UI, который разрешается в строку на экране, а не во ViewModel. Строка, превращённая в
 * `String` при записи в State, остаётся на языке момента записи (ViewModel переживает смену
 * локали); `UiText` хранит причину, и локализуется при отрисовке.
 *
 * Разрешает его `@Composable UiText.asString()` из `core:designsystem`.
 */
sealed interface UiText {

    /** Готовая строка без локализации (текст сервера, имя, значение из данных). */
    data class Plain(val value: String) : UiText

    /** Строковый ресурс [resId] (`R.string.*`) с форматирующими [args]. */
    data class Res(val resId: Int, val args: List<Any> = emptyList()) : UiText

    /**
     * Сбой загрузки: локализованный текст по типу исключения (сеть, таймаут, HTTP-код), иначе
     * текст самого исключения, иначе [fallback].
     */
    data class Failure(val cause: Throwable, val fallback: UiText? = null) : UiText
}
