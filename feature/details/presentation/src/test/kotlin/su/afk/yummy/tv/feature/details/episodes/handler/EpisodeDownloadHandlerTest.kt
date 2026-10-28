package su.afk.yummy.tv.feature.details.episodes.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnailResolver
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.usecase.ResolvePlayerStreamUseCase
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.usecase.CancelOrDeleteVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.EnqueueVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.PrepareVideoDownloadQualityOptionsUseCase
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.episodes.EpisodesState.EpisodeDownloadUiStatus
import su.afk.yummy.tv.feature.details.presentation.R

class EpisodeDownloadHandlerTest : BaseUnitTest() {

    private val resolvePlayerStream: ResolvePlayerStreamUseCase = mockk()
    private val enqueueVideoDownload: EnqueueVideoDownloadUseCase = mockk(relaxed = true)
    private val cancelOrDelete: CancelOrDeleteVideoDownloadUseCase = mockk(relaxed = true)
    private val thumbnailResolver: EpisodeThumbnailResolver = mockk()
    private val strings: StringProvider = mockk()

    private val kodik = video(id = 1, dubbing = "AniLibria", player = "Kodik", views = 10, url = KODIK_URL)
    private val alloha = video(id = 2, dubbing = "AniLibria", player = "Alloha", views = 5, url = ALLOHA_URL)
    private val dream = video(id = 3, dubbing = "Dream Cast", player = "Kodik", views = 40, url = "https://kodik.info/3")

    @Before
    fun setUp() {
        every { strings.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        coEvery { thumbnailResolver.resolveUrl(any()) } returns THUMBNAIL
    }

    // ---------------- pickers ----------------

    @Test
    fun `dubbings are grouped, sorted by views and list their players`() {
        val selection = createHandler().dubbingSelection(listOf(kodik, alloha, dream), emptyMap(), emptySet())

        assertEquals(listOf("Dream Cast", "AniLibria"), selection.options.map { it.title })
        assertEquals("Kodik • Alloha", selection.options[1].subtitle)
        assertEquals("3", selection.episode)
        assertFalse(selection.hasAlternativeDubbings)
    }

    @Test
    fun `excluded dubbing is left out and alternatives are flagged`() {
        val selection = createHandler().dubbingSelection(
            listOf(kodik, alloha, dream),
            emptyMap(),
            resolvingKeys = setOf(key(dream)),
            excludedDubbing = "AniLibria",
        )

        assertEquals(listOf("Dream Cast"), selection.options.map { it.title })
        assertTrue(selection.options.single().resolving)
        assertTrue(selection.hasAlternativeDubbings)
    }

    @Test
    fun `dubbing status is shown only when all its sources agree`() {
        val handler = createHandler()
        val done = mapOf(key(kodik) to status(EpisodeDownloadUiStatus.Downloaded), key(alloha) to status(EpisodeDownloadUiStatus.Downloaded))
        val mixed = mapOf(key(kodik) to status(EpisodeDownloadUiStatus.Downloaded))

        val agreed = handler.dubbingSelection(listOf(kodik, alloha), done, emptySet()).options.single()
        val partial = handler.dubbingSelection(listOf(kodik, alloha), mixed, emptySet()).options.single()

        assertEquals(EpisodeDownloadUiStatus.Downloaded, agreed.status?.status)
        assertNull(partial.status)
    }

    @Test
    fun `balancers are sorted by views and carry their own status`() {
        val selection = createHandler().balancerSelection(
            listOf(alloha, kodik),
            statuses = mapOf(key(alloha) to status(EpisodeDownloadUiStatus.Failed)),
            resolvingKeys = setOf(key(kodik)),
        )

        assertEquals(listOf("Kodik", "Alloha"), selection.options.map { it.title })
        assertTrue(selection.options[0].resolving)
        assertEquals(EpisodeDownloadUiStatus.Failed, selection.options[1].status?.status)
        assertEquals("AniLibria", selection.dubbing)
    }

    @Test
    fun `extra 4k source goes after yani balancers despite more views`() {
        val fourK = video(id = 4, dubbing = "AniLibria", player = "4K", views = 1_000_000, url = "source://4k/1")

        val selection = createHandler().balancerSelection(
            listOf(alloha, kodik, fourK),
            statuses = emptyMap(),
            resolvingKeys = emptySet(),
        )

        assertEquals(listOf("Kodik", "Alloha", "4K"), selection.options.map { it.title })
    }

    @Test
    fun `balancer views are summed over the whole title`() {
        val otherEpisodeAlloha = video(id = 5, dubbing = "Dream Cast", player = "Alloha", views = 100, url = ALLOHA_URL)

        val selection = createHandler().balancerSelection(
            listOf(kodik, alloha),
            statuses = emptyMap(),
            resolvingKeys = emptySet(),
            allVideos = listOf(kodik, alloha, otherEpisodeAlloha),
        )

        assertEquals(listOf("Alloha", "Kodik"), selection.options.map { it.title })
    }

    // ---------------- prepare ----------------

    @Test
    fun `resolved stream offers its qualities`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns stream("480p", "1080p")

        val result = createHandler().prepare(kodik) as EpisodeDownloadPrepareResult.Ready

        assertEquals(key(kodik), result.key)
        assertEquals(listOf("480p", "1080p"), result.selection.options.map { it.label })
    }

    @Test
    fun `alloha without numeric qualities cannot be downloaded`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns stream("Auto")

        val result = createHandler().prepare(alloha)

        assertEquals(EpisodeDownloadPrepareResult.Failure(key(alloha), "res${R.string.details_download_resolve_error}"), result)
    }

    @Test
    fun `source refusals and crashes become readable failures`() = runTest {
        val handler = createHandler()

        coEvery { resolvePlayerStream(any()) } returns PlayerStreamResolveResult.KodikBlocked(message = "blocked")
        assertEquals("blocked", (handler.prepare(kodik) as EpisodeDownloadPrepareResult.Failure).message)

        coEvery { resolvePlayerStream(any()) } returns PlayerStreamResolveResult.Unavailable()
        assertEquals(
            "res${R.string.details_download_dubbing_unavailable}",
            (handler.prepare(kodik) as EpisodeDownloadPrepareResult.Failure).message,
        )

        coEvery { resolvePlayerStream(any()) } throws IllegalStateException("timeout")
        assertEquals("timeout", (handler.prepare(kodik) as EpisodeDownloadPrepareResult.Failure).message)
    }

    // ---------------- enqueue ----------------

    @Test
    fun `enqueue without a prepared candidate or with an unknown quality is refused`() = runTest {
        val handler = createHandler()
        assertEquals(EpisodeDownloadEnqueueResult.MissingCandidate, enqueue(handler, "720p"))

        coEvery { resolvePlayerStream(any()) } returns stream("1080p")
        handler.prepare(kodik)

        assertEquals(EpisodeDownloadEnqueueResult.MissingCandidate, enqueue(handler, "720p"))
    }

    @Test
    fun `prepared quality is enqueued with the stream headers and a kodik thumbnail`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns stream("1080p")
        val request = slot<VideoDownloadRequest>()
        coEvery { enqueueVideoDownload(capture(request)) } returns mockk()
        val handler = createHandler()
        handler.prepare(kodik)

        val result = enqueue(handler, "1080p")

        assertEquals(EpisodeDownloadEnqueueResult.Success, result)
        assertEquals(ANIME_ID, request.captured.animeId)
        assertEquals(kodik.id, request.captured.videoId)
        assertEquals("1080p", request.captured.quality.label)
        assertEquals(mapOf("Referer" to "kodik"), request.captured.headers)
        assertEquals(THUMBNAIL, request.captured.screenshotUrl)
        // Кандидат одноразовый.
        assertEquals(EpisodeDownloadEnqueueResult.MissingCandidate, enqueue(handler, "1080p"))
    }

    @Test
    fun `replacement deletes the old download before enqueueing`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns stream("1080p")
        val handler = createHandler()
        handler.beginReplacement(OLD_DOWNLOAD_ID)
        handler.prepare(kodik)

        enqueue(handler, "1080p")

        coVerifyOrder {
            cancelOrDelete(OLD_DOWNLOAD_ID)
            enqueueVideoDownload(any())
        }
    }

    @Test
    fun `failed replacement delete keeps the old download and enqueues nothing`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns stream("1080p")
        coEvery { cancelOrDelete(OLD_DOWNLOAD_ID) } throws IllegalStateException("io")
        val handler = createHandler()
        handler.beginReplacement(OLD_DOWNLOAD_ID)
        handler.prepare(kodik)

        assertEquals(EpisodeDownloadEnqueueResult.ReplacementDeleteFailed, enqueue(handler, "1080p"))
        coVerify(exactly = 0) { enqueueVideoDownload(any()) }
    }

    @Test
    fun `failed enqueue is reported`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns stream("1080p")
        coEvery { enqueueVideoDownload(any()) } throws IllegalStateException("db")
        val handler = createHandler()
        handler.prepare(kodik)

        assertEquals(EpisodeDownloadEnqueueResult.EnqueueFailed, enqueue(handler, "1080p"))
    }

    private suspend fun enqueue(handler: EpisodeDownloadHandler, label: String) = handler.enqueue(
        option = EpisodesState.EpisodeDownloadQualityOption(label, "https://cdn/$label.m3u8"),
        animeId = ANIME_ID,
        animeTitle = "Title",
        posterUrl = "poster",
        episodeVideos = listOf(kodik, alloha),
    )

    private fun stream(vararg labels: String) = PlayerStreamResolveResult.Stream(
        url = "https://cdn/master.m3u8",
        headers = mapOf("Referer" to "kodik"),
        qualities = linkedMapOf(*labels.map { it to "https://cdn/$it.m3u8" }.toTypedArray()),
    )

    private fun status(status: EpisodeDownloadUiStatus) = EpisodesState.EpisodeDownloadUiState(
        downloadId = 1L,
        dubbing = "AniLibria",
        playerName = "Kodik",
        qualityLabel = "720p",
        bytesDownloaded = 0L,
        status = status,
        progress = 0f,
        errorMessage = null,
    )

    private fun key(video: AnimeVideo) = "${video.id}|${video.iframeUrl}"

    private fun video(id: Int, dubbing: String, player: String, views: Int, url: String) = AnimeVideo(
        id = id,
        episode = "3",
        dubbing = dubbing,
        player = player,
        playerId = null,
        iframeUrl = url,
        durationSeconds = 1440,
        views = views,
    )

    private fun createHandler() = EpisodeDownloadHandler(
        resolvePlayerStream = resolvePlayerStream,
        prepareDownloadQualities = PrepareVideoDownloadQualityOptionsUseCase(),
        enqueueVideoDownload = enqueueVideoDownload,
        cancelOrDeleteVideoDownload = cancelOrDelete,
        thumbnailResolver = thumbnailResolver,
        strings = strings,
    )

    private companion object {
        const val ANIME_ID = 42
        const val OLD_DOWNLOAD_ID = 9L
        const val KODIK_URL = "https://kodik.info/1"
        const val ALLOHA_URL = "https://alloha.tv/2"
        const val THUMBNAIL = "https://kodik.info/thumb.jpg"
    }
}
