package su.afk.yummy.tv.source.torrent.data.source

/** Что умеет декодировать это устройство: нужно, чтобы предупредить о раздачах, которые на нём не заиграют. */
internal interface VideoDecoderSupport {
    /** Есть декодер HEVC Main10 (10 бит): в таком виде выходят раздачи AniLibria/AniLiberty. */
    fun supportsHevcMain10(): Boolean
}
