package su.afk.yummy.tv.feature.player.common.utils

import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import su.afk.yummy.tv.feature.player.common.service.DeblurGlEffect

/**
 * Цепочка эффектов Deblur: только резкость с порогом на исходном разрешении (там подобраны сила и порог).
 * Апскейла до экрана в цепочке нет: бикубик на 4K-кадре (16 выборок на пиксель) не тянут GPU ТВ-приставок,
 * картинка отстаёт от звука. До размера экрана кадр растягивает аппаратный скейлер вывода — без нагрузки на GPU.
 */
@OptIn(UnstableApi::class)
internal fun deblurVideoEffects(): List<Effect> = listOf(DeblurGlEffect())
