package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.gif.repeatCount
import coil3.request.ImageRequest
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme

/** Анимированный WebP декора (шляпа, росчерк, хвост) с сервера, папка static. */
private const val HalloweenAvatarDecorUrl = "https://yummytv.afk.su/static/halloween-avatar-decor.webp"

/** Во сколько раз квадрат декора больше аватарки: аватарка занимает 240 из 288 px кадра. */
private const val AvatarDecorScale = 1.2f

/**
 * Область аватарки с сезонным декором. На Хэллоуин это квадрат, в который целиком вписан анимированный декор, а
 * круглая аватарка из [content] лежит внутри и сохраняет размер [avatarSize]. В остальное время — квадрат
 * [avatarSize] (на Новый год с шапкой Деда Мороза). [content] сам скругляет аватарку.
 */
@Composable
fun SeasonalAvatarFrame(
    avatarSize: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (rememberSeasonalTheme() != SeasonalTheme.HALLOWEEN) {
        Box(
            modifier = modifier.size(avatarSize).seasonalAvatarHat(),
            contentAlignment = Alignment.Center,
        ) { content() }
        return
    }
    val animated = rememberSeasonalOverlayEnabled()
    val request = ImageRequest.Builder(LocalContext.current)
        .data(HalloweenAvatarDecorUrl)
        // Без анимаций проигрываем один раз и замираем на последнем кадре (шляпа и хвост).
        .apply { if (!animated) repeatCount(0) }
        .build()
    Box(
        modifier = modifier.size(avatarSize * AvatarDecorScale),
        contentAlignment = Alignment.Center,
    ) {
        content()
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
