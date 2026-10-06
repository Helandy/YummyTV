package su.afk.yummy.tv.domain.home.utils

import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleItem

/**
 * Когда у тайтла вышла последняя серия, epoch-секунды, или `null`, если расписание этого не знает.
 *
 * Основной источник — `prev_date`, но у части тайтлов он нулевой, зато `next_date` уже в прошлом:
 * сервер не передвинул его после выхода серии. В таком случае прошедший `next_date` и есть дата
 * последней вышедшей серии. Будущий `prev_date` (в выдаче такие встречаются) отбрасываем.
 *
 * У анонсов без вышедших серий `prev_date` всё равно заполнен — датой-заглушкой, общей для всей
 * пачки тайтлов (проверено вживую 06.10.2026: один и тот же `prev_date` у 30 записей, среди них
 * `announcement`). Поэтому дату отдаём только там, где серии действительно выходили: `aired > 0`,
 * то есть [AnimeScheduleItem.airedEpisodes] не `null`.
 */
fun AnimeScheduleItem.lastAiredSeconds(nowSeconds: Long): Long? {
    if (airedEpisodes == null) return null
    return listOfNotNull(previousDateEpochSeconds, nextDateEpochSeconds)
        .filter { it in 1..nowSeconds }
        .maxOrNull()
}
