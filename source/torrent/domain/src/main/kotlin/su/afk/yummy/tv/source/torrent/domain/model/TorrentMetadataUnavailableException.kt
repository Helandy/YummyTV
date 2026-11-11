package su.afk.yummy.tv.source.torrent.domain.model

/**
 * TorrServer принял раздачу, но список файлов не пришёл за отведённое время: у раздачи нет доступных пиров
 * (трекер может показывать сидов, до которых клиент не достучался). Отличается от «нет видеофайлов» и от
 * «сервер недоступен»: пользователю советуют выбрать другую раздачу или повторить позже.
 */
class TorrentMetadataUnavailableException(val hash: String) : Exception("No torrent metadata for $hash")
