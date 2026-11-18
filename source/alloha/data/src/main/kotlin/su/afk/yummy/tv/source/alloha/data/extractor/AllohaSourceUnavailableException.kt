package su.afk.yummy.tv.source.alloha.data.extractor

/** Alloha resolved the session but reports no HLS source for the requested dubbing/episode. */
internal class AllohaSourceUnavailableException(message: String? = null) : Exception(message)
