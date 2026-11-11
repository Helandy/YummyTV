package su.afk.yummy.tv.source.torrent.data.source

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import javax.inject.Inject
import javax.inject.Singleton

/** Читает список декодеров системы один раз: он не меняется, пока приложение работает. */
@Singleton
internal class AndroidVideoDecoderSupport @Inject constructor() : VideoDecoderSupport {

    private val hevcMain10: Boolean by lazy {
        runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals(HEVC_MIME, ignoreCase = true) } &&
                    info.getCapabilitiesForType(HEVC_MIME).profileLevels.any { it.supportsMain10For1080p() }
            }
        }.getOrDefault(true) // не смогли узнать — не пугаем пользователя
    }

    override fun supportsHevcMain10(): Boolean = hevcMain10

    /** Main10 хотя бы до уровня 4.0 (1080p24); раздачи AniLibria на 5.1 тянут не все, но предупреждаем только о явном «нет». */
    private fun CodecProfileLevel.supportsMain10For1080p(): Boolean =
        (profile == CodecProfileLevel.HEVCProfileMain10 || profile == CodecProfileLevel.HEVCProfileMain10HDR10) &&
            level >= CodecProfileLevel.HEVCMainTierLevel4

    private companion object {
        const val HEVC_MIME = "video/hevc"
    }
}
