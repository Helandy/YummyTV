package su.afk.yummy.tv.feature.player.common.service

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSchemeDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * Направляет `data:`-URI (синтетический HLS master) в [dataUriSource], всё остальное — в [other]
 * (кэш и сеть). Выбор делается при каждом open(), поэтому один экземпляр обслуживает и master, и сегменты.
 */
@OptIn(UnstableApi::class)
internal class SchemeRoutingDataSource(
    private val dataUriSource: DataSchemeDataSource,
    private val other: DataSource,
) : DataSource {
    private var active: DataSource? = null
    private val listeners = mutableListOf<TransferListener>()

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
        dataUriSource.addTransferListener(transferListener)
        other.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val source = if (dataSpec.uri.scheme.equals("data", ignoreCase = true)) dataUriSource else other
        active = source
        return source.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(active).read(buffer, offset, length)

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = active?.responseHeaders.orEmpty()

    override fun close() {
        try {
            active?.close()
        } finally {
            active = null
        }
    }
}
