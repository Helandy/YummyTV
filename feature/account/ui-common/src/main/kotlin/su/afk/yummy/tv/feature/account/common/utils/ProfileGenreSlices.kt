package su.afk.yummy.tv.feature.account.common.utils

import androidx.compose.ui.graphics.Color
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.common.model.ProfileStatSlice

/** Сколько крупнейших жанров показываем отдельными строками; остальные сворачиваются в «Другие». */
const val PROFILE_GENRE_TOP_COUNT = 5

private val OtherGenresColor = Color(0xFF8F939A)

/**
 * Жанры по убыванию количества: топ-[PROFILE_GENRE_TOP_COUNT] и строка [otherTitle] с суммой остальных.
 * Если жанров не больше, чем строк в легенде, сворачивать нечего и показываются все.
 */
fun UserStats.genreCountSlices(otherTitle: String): List<ProfileStatSlice> {
    val sorted = genres.filter { it.count > 0 }.sortedByDescending { it.count }
    val collapse = sorted.size > PROFILE_GENRE_TOP_COUNT + 1
    val top = if (collapse) sorted.take(PROFILE_GENRE_TOP_COUNT) else sorted
    val slices = top.mapIndexed { index, item ->
        ProfileStatSlice(
            title = item.title,
            value = item.count.toLong(),
            color = fallbackProfileColors[index % fallbackProfileColors.size],
        )
    }
    if (!collapse) return slices
    val otherSum = sorted.drop(PROFILE_GENRE_TOP_COUNT).sumOf { it.count.toLong() }
    return slices + ProfileStatSlice(title = otherTitle, value = otherSum, color = OtherGenresColor)
}

/** Жанры, в которых меньше стольких тайтлов, сворачиваются в «Другие». */
const val PROFILE_GENRE_MIN_COUNT = 5

/**
 * Все жанры по убыванию, у каждого свой цвет: сначала палитра профиля, дальше оттенки по золотому
 * углу, чтобы соседние сегменты не сливались даже при двух десятках жанров. Жанры с количеством
 * меньше [PROFILE_GENRE_MIN_COUNT] суммируются в последнюю строку [otherTitle]; один такой жанр
 * остаётся как есть — «Другие» из одного жанра ничего не сворачивают.
 */
fun UserStats.genreAllSlices(otherTitle: String): List<ProfileStatSlice> {
    val sorted = genres.filter { it.count > 0 }.sortedByDescending { it.count }
    val (major, minor) = sorted.partition { it.count >= PROFILE_GENRE_MIN_COUNT }
    val collapse = minor.size > 1
    val shown = if (collapse) major else sorted
    val slices = shown.mapIndexed { index, item ->
        ProfileStatSlice(
            title = item.title,
            value = item.count.toLong(),
            color = genreColor(index),
        )
    }
    if (!collapse) return slices
    return slices + ProfileStatSlice(
        title = otherTitle,
        value = minor.sumOf { it.count.toLong() },
        color = OtherGenresColor,
    )
}

private const val GOLDEN_ANGLE_DEGREES = 137.508f

private fun genreColor(index: Int): Color =
    if (index < fallbackProfileColors.size) {
        fallbackProfileColors[index]
    } else {
        Color.hsv(
            hue = ((index - fallbackProfileColors.size) * GOLDEN_ANGLE_DEGREES + 20f) % 360f,
            saturation = 0.55f,
            value = 0.92f,
        )
    }
