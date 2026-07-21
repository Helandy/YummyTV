package su.afk.yummy.tv.feature.reviews.mobile.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent

@Composable
internal fun ReviewMobileRemoteImage(url: String, alt: String) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = alt,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        val state by painter.state.collectAsState()
        when (state) {
            is AsyncImagePainter.State.Loading -> Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is AsyncImagePainter.State.Error -> Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                contentAlignment = Alignment.Center
            ) { Text(alt) }

            else -> SubcomposeAsyncImageContent()
        }
    }
}
