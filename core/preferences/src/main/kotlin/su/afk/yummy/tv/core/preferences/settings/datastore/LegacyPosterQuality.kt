package su.afk.yummy.tv.core.preferences.settings.datastore

import android.os.Build
import su.afk.yummy.tv.core.model.settings.PosterQuality

/**
 * Качество постеров, которое до появления пробы форматов получали все, кто его не выбирал сами:
 * на Android 12+ хватает памяти на максимальное, на старых — стандартное.
 * Используется только для закрепления значения у обновляющихся пользователей.
 */
internal fun legacyPosterQuality(sdkInt: Int): PosterQuality =
    if (sdkInt >= Build.VERSION_CODES.S) PosterQuality.MEGA else PosterQuality.STANDARD
