package su.afk.yummy.tv.feature.details.mobile.details.model

import androidx.compose.ui.graphics.vector.ImageVector

/** Плоская строка-ссылка внизу деталки; [badge] — необязательный счётчик перед шевроном. */
internal data class DetailsLink(
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val onClick: () -> Unit,
    val badge: String? = null,
    /** Подчёркнутое название — для плоских ссылок без иконки-кнопки, чтобы было видно, что строка кликабельна. */
    val underlined: Boolean = false,
)
