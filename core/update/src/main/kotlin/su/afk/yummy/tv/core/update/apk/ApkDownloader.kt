package su.afk.yummy.tv.core.update.apk

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ApkDownloader(private val context: Context) {

    suspend fun download(url: String, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val outputFile = File(context.cacheDir, "update.apk")

            var connection = URL(url).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connect()

            // manual redirect follow for cross-scheme redirects (HTTPS → HTTPS)
            var redirects = 0
            while (connection.responseCode in 300..399 && redirects < 5) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                connection = URL(location).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connect()
                redirects++
            }

            val contentLength = connection.contentLengthLong
            var bytesRead = 0L
            val buffer = ByteArray(8192)

            connection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        output.write(buffer, 0, n)
                        bytesRead += n
                        if (contentLength > 0) {
                            onProgress(bytesRead.toFloat() / contentLength.toFloat())
                        }
                    }
                }
            }
            connection.disconnect()

            outputFile
        }
}