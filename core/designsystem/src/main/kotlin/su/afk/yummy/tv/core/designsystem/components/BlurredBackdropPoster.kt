package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

private val BackdropBlurRadius = 28.dp
private const val BackdropScrimAlpha = 0.35f

/**
 * Постер целиком (без обрезки) поверх размытой копии самого себя.
 *
 * Размер, форму и клики задаёт вызывающий через [modifier]. Свободные поля рамки
 * заполняет размытый и затемнённый постер; на API < 31 blur ничего не делает,
 * поэтому подложкой служит цвет surfaceVariant. Оба слоя берут картинку по одному URL
 * и делят memory-cache ключ из [CachedAsyncImage], так что грузится она один раз.
 */
@Composable
fun BlurredBackdropPoster(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clipToBounds()) {
        CachedAsyncImage(
            url = url,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .blur(BackdropBlurRadius),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = BackdropScrimAlpha)),
        )
        CachedAsyncImage(
            url = url,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}
