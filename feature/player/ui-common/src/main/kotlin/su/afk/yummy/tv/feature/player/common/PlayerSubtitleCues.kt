package su.afk.yummy.tv.feature.player.common

import androidx.media3.common.text.Cue

/**
 * Готовит реплики [androidx.media3.common.text.CueGroup] к отрисовке в [PlayerSubtitleOverlay].
 *
 * Media3 [androidx.media3.ui.SubtitleView] рисует каждый cue отдельным painter'ом и не разводит их
 * по вертикали. Поскольку явные line/position мы всё равно сбрасываем (иначе они перебивают
 * пользовательский offset), все одновременные реплики оказались бы в одной точке у нижнего края и
 * наслаивались друг на друга. Поэтому текстовые cue склеиваем в один — их раскладывает единый
 * Layout, и реплики встают друг над другом.
 *
 * Битмап-cue (например, DVB) не трогаем: там позиция осмысленная и в текст не сводится.
 */
internal fun List<Cue>.mergeSimultaneousCues(): List<Cue> {
    if (isEmpty()) return this

    val bitmapCues = filter { it.bitmap != null }
    val lines = subtitleLines()
    if (lines.isEmpty()) return bitmapCues

    val mergedCue = first { it.bitmap == null }.buildUpon()
        .setText(lines.joinToString(separator = "\n"))
        .setLine(Cue.DIMEN_UNSET, Cue.TYPE_UNSET)
        .setPosition(Cue.DIMEN_UNSET)
        .build()

    return if (bitmapCues.isEmpty()) listOf(mergedCue) else listOf(mergedCue) + bitmapCues
}

/**
 * Тексты одновременных реплик в порядке появления, без пустых и без повторов: ASS-дорожки часто
 * дублируют реплику слоями (обводка, эффекты), а встроенные стили мы всё равно не применяем — такие
 * слои рисуются одинаково и выглядят как «жирный» двойной текст.
 */
internal fun List<Cue>.subtitleLines(): List<String> = asSequence()
    .filter { it.bitmap == null }
    .mapNotNull { it.text?.toString()?.trim() }
    .filter { it.isNotEmpty() }
    .distinct()
    .toList()
