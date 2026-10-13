package su.afk.yummy.tv.feature.update.handler

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.update.repository.ApkInstallRepository
import su.afk.yummy.tv.domain.update.usecase.ObserveUpdateDownloadUseCase
import su.afk.yummy.tv.domain.update.usecase.StartUpdateDownloadUseCase
import su.afk.yummy.tv.feature.update.UpdateAnalytics
import java.io.File

class UpdateInstallHandlerTest : BaseUnitTest() {

    private val apkInstallRepository: ApkInstallRepository = mockk(relaxed = true)
    private val analytics: UpdateAnalytics = mockk(relaxed = true)
    private val apk = File("update.apk")

    @Test
    fun `installed apk is reported as success`() = runTest {
        assertEquals(UpdateInstallResult.Success(apk), createHandler().install(apk, "1.26.0"))
    }

    @Test
    fun `install failure is reported to analytics and returned`() = runTest {
        val error = SecurityException("unknown sources")
        coEvery { apkInstallRepository.install(apk) } throws error

        val result = createHandler().install(apk, "1.26.0")

        assertEquals(UpdateInstallResult.Failure(error), result)
        verify { analytics.eventInstallError("1.26.0", error) }
    }

    private fun createHandler() = UpdateInstallHandler(
        startUpdateDownload = mockk<StartUpdateDownloadUseCase>(),
        observeUpdateDownload = mockk<ObserveUpdateDownloadUseCase>(),
        apkInstallRepository = apkInstallRepository,
        analytics = analytics,
    )
}
