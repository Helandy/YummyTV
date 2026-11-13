package su.afk.yummy.tv.android.image

import android.content.Context
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.utils.system.ImageCacheCleaner
import javax.inject.Inject
import javax.inject.Singleton

/** Чистит память и диск singleton [coil3.ImageLoader] (см. [CoilImageLoaderInstaller]). */
@Singleton
internal class CoilImageCacheCleaner @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ImageCacheCleaner {

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            val loader = SingletonImageLoader.get(context)
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
        }
    }
}
