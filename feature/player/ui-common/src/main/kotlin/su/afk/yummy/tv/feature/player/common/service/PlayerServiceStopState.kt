package su.afk.yummy.tv.feature.player.common.service

/**
 * Ставится до stopSelf(): после него сервис не должен снова уходить в foreground. Снимается,
 * если к ещё не снесённому экземпляру подключился новый экран плеера.
 */
internal class PlayerServiceStopState {
    var isStopping: Boolean = false
}
