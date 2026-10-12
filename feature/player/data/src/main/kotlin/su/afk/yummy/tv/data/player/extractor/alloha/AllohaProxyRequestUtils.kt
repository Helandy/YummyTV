package su.afk.yummy.tv.data.player.extractor.alloha

import java.io.BufferedReader
import java.io.IOException

/**
 * Читает строку HTTP-запроса, ограничивая её длину: [BufferedReader.readLine] без лимита копит
 * строку в памяти, пока не придёт перевод строки, и зависший или чужой клиент на loopback-порту
 * мог бы раздуть кучу. Длинные значения (`url=` с base64) укладываются в [maxLength] с запасом.
 *
 * @return строка без перевода строки или `null`, если поток закончился до первого символа.
 * @throws IOException если строка длиннее [maxLength].
 */
internal fun BufferedReader.readLineBounded(maxLength: Int): String? {
    val builder = StringBuilder()
    while (true) {
        val code = read()
        if (code == -1) return builder.takeIf { it.isNotEmpty() }?.toString()
        when (val char = code.toChar()) {
            '\n' -> return builder.toString().removeSuffix("\r")
            else -> {
                if (builder.length >= maxLength) throw IOException("HTTP line exceeds $maxLength chars")
                builder.append(char)
            }
        }
    }
}
