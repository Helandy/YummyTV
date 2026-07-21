package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadQualityOption
import javax.inject.Inject

/** Формирует доступные варианты качества для офлайн-загрузки. */
class PrepareVideoDownloadQualityOptionsUseCase @Inject constructor() {
    operator fun invoke(
        streamUrl: String,
        qualityMap: LinkedHashMap<String, String>?,
        qualityHeaders: Map<String, Map<String, String>> = emptyMap(),
        numericQualitiesOnly: Boolean = false,
    ): List<VideoDownloadQualityOption> {
        val mapped = qualityMap
            ?.filter { (label, url) ->
                label.isNotBlank() &&
                        url.isNotBlank() &&
                        (!numericQualitiesOnly || label.hasVideoQualityNumber())
            }
            ?.map { (label, url) ->
                VideoDownloadQualityOption(
                    label = label,
                    url = url,
                    headers = qualityHeaders[label].orEmpty(),
                )
            }
            .orEmpty()
        return mapped.ifEmpty {
            if (numericQualitiesOnly) return emptyList()
            listOf(VideoDownloadQualityOption(label = "Auto", url = streamUrl))
        }
    }

    private fun String.hasVideoQualityNumber(): Boolean =
        VIDEO_QUALITY_REGEX.containsMatchIn(this)

    private companion object {
        val VIDEO_QUALITY_REGEX = Regex("""\d{3,4}p?""", RegexOption.IGNORE_CASE)
    }
}
