package su.afk.yummy.tv.feature.player.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.mapper.loadingTextRes
import su.afk.yummy.tv.feature.player.model.PlayerResolveLogEntry
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.toElapsedText
import su.afk.yummy.tv.feature.player.utils.toText

private const val VISIBLE_ENTRIES = 6
private const val SLOW_STEP_AFTER_SECONDS = 3
private const val TICK_MS = 1000L

/**
 * Журнал получения потока под спиннером: пройденные шаги и сетевые запросы (хост, HTTP-код, время).
 * Если текущий шаг тянется дольше нескольких секунд, рядом бежит счётчик ожидания.
 */
@Composable
fun PlayerResolveLogView(
    log: List<PlayerResolveLogEntry>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
) {
    if (log.isEmpty()) return
    val lastStep = log.lastOrNull { it.status != null }
    val waitedSeconds by produceState(0, lastStep) {
        value = 0
        while (true) {
            delay(TICK_MS)
            value += 1
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        log.takeLast(VISIBLE_ENTRIES).forEach { entry ->
            val text = entry.status?.let { status ->
                val base = "${entry.elapsedMs.toElapsedText()}  ${stringResource(status.loadingTextRes())}"
                if (entry === lastStep && waitedSeconds >= SLOW_STEP_AFTER_SECONDS) {
                    "$base ${stringResource(R.string.player_resolve_waiting_seconds, waitedSeconds)}"
                } else {
                    base
                }
            } ?: entry.detail?.let { "      ${it.toText()}" }.orEmpty()
            Text(
                text = text,
                color = Color.White.copy(alpha = if (entry.status != null) STEP_ALPHA else DETAIL_ALPHA),
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Start,
                maxLines = 1,
            )
        }
    }
}

private const val STEP_ALPHA = 0.75f
private const val DETAIL_ALPHA = 0.5f
