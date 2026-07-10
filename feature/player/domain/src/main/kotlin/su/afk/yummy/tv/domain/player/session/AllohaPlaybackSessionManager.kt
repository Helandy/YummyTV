package su.afk.yummy.tv.domain.player.session

import su.afk.yummy.tv.domain.player.model.AllohaStreamSession

/** Owns the live Alloha session independently from a particular player UI instance. */
interface AllohaPlaybackSessionManager {
    fun find(sourceKey: String): AllohaStreamSession?
    fun activate(session: AllohaStreamSession): AllohaStreamSession
    fun release(session: AllohaStreamSession, immediately: Boolean)
    fun closeActive()
}
