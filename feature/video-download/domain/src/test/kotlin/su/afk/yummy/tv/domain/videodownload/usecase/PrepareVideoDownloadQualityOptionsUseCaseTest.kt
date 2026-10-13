package su.afk.yummy.tv.domain.videodownload.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadQualityOption

class PrepareVideoDownloadQualityOptionsUseCaseTest : BaseUnitTest() {

    private val useCase = PrepareVideoDownloadQualityOptionsUseCase()

    @Test
    fun `qualities keep their order and per-quality headers, blanks are dropped`() {
        val options = useCase(
            streamUrl = MASTER,
            qualityMap = linkedMapOf("360p" to "u360", "" to "u0", "720p" to "u720", "1080p" to ""),
            qualityHeaders = mapOf("720p" to mapOf("Referer" to "r")),
        )

        assertEquals(
            listOf(
                VideoDownloadQualityOption("360p", "u360"),
                VideoDownloadQualityOption("720p", "u720", mapOf("Referer" to "r")),
            ),
            options,
        )
    }

    @Test
    fun `stream without qualities is offered as auto`() {
        assertEquals(listOf(VideoDownloadQualityOption("Auto", MASTER)), useCase(MASTER, null))
    }

    @Test
    fun `numeric-only mode drops labels without a resolution and offers no auto`() {
        val options = useCase(
            streamUrl = MASTER,
            qualityMap = linkedMapOf("Auto" to "uAuto", "1080" to "u1080"),
            numericQualitiesOnly = true,
        )

        assertEquals(listOf("1080"), options.map { it.label })
        assertTrue(useCase(MASTER, linkedMapOf("Auto" to "uAuto"), numericQualitiesOnly = true).isEmpty())
    }

    private companion object {
        const val MASTER = "https://cdn/master.m3u8"
    }
}
