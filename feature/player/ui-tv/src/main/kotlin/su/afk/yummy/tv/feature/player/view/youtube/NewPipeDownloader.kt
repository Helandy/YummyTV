package su.afk.yummy.tv.feature.player.view.youtube

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.HttpURLConnection
import java.net.URL

class NewPipeDownloader private constructor() : Downloader() {

    override fun execute(request: Request): Response {
        val conn = URL(request.url()).openConnection() as HttpURLConnection
        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")

        request.headers().forEach { (key, values) ->
            values.forEach { value -> conn.setRequestProperty(key, value) }
        }

        request.dataToSend()?.let { data ->
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.outputStream.use { it.write(data) }
        }

        val code = conn.responseCode
        val body = runCatching { conn.inputStream.bufferedReader().readText() }
            .getOrElse { conn.errorStream?.bufferedReader()?.readText() ?: "" }
        val headers = conn.headerFields.filterKeys { it != null }.mapValues { it.value }

        return Response(code, conn.responseMessage, headers, body, request.url())
    }

    companion object {
        val instance = NewPipeDownloader()
    }
}
