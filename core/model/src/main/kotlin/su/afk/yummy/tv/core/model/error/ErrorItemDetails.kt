package su.afk.yummy.tv.core.model.error

import su.afk.yummy.tv.core.model.ErrorItem

private const val MAX_CAUSE_LENGTH = 240

/** Текст для экрана ошибки: заголовок, сообщение и техническая причина, чтобы её можно было прислать разработчику. */
fun ErrorItem.toDisplayDetails(): String = listOfNotNull(
    title,
    message.takeIf { it.isNotBlank() && it != title },
    cause?.takeIf { it.isNotBlank() }?.take(MAX_CAUSE_LENGTH),
).joinToString("\n")
