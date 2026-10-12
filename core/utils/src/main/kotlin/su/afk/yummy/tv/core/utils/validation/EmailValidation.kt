package su.afk.yummy.tv.core.utils.validation

// Тот же шаблон, что и `android.util.Patterns.EMAIL_ADDRESS`: проверка e-mail не должна тянуть
// Android-классы в presentation-слой и работает в JVM-тестах.
private val EMAIL_REGEX = Regex(
    "[a-zA-Z0-9+._%\\-]{1,256}" +
        "@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+",
)

/** Похоже ли значение на адрес электронной почты (полное совпадение с шаблоном). */
fun String.isValidEmail(): Boolean = EMAIL_REGEX.matches(this)
