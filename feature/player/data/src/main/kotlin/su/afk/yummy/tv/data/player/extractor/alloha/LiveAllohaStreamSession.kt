package su.afk.yummy.tv.data.player.extractor.alloha

import android.os.Handler
import android.webkit.CookieManager
import android.webkit.WebView
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private const val LOG_TAG = PersistedLogTags.ALLOHA_EXTRACTOR

// How long a rotation may stay staged before we apply whatever the reload managed to produce.
// Committing a partial state is the pre-existing behaviour (a short stall while the proxy
// recovers), so this is a floor on quality, not a new failure mode - but it must stay short
// enough that the previous token, refreshed SESSION_REFRESH_LEAD_MS (20s) before it expires, is
// still valid when the forced commit lands.
private const val STAGED_COMMIT_TIMEOUT_MS = 8_000L

/**
 * Mutable state of one live Alloha session: the anti-bot headers, the currently signed master URL,
 * its TTL and a monotonically growing [generation] the proxy uses to notice that the session was
 * replaced. Also owns the selected dubbing/quality and the loopback proxy serving them.
 *
 * The tricky part is rotation. A WebView reload re-emits its signals in stages, so writing them
 * straight into the live state would leave the proxy serving a half-updated, unauthorized session
 * for that whole window. Instead every update during a rotation lands in a shadow [StagedState] and
 * is swapped in atomically - the still-valid previous token keeps serving until then. See
 * `docs/alloha-player.md` for how this fits the rest, and [sawConfigUpdate] for why the rotation
 * cannot wait on the signal it was originally designed around.
 */
internal class LiveAllohaStreamSession(
    private val handler: Handler,
    override val sourceKey: String,
    private val analytics: AnalyticsTracker,
) : AllohaStreamSession {
    override val id: String = UUID.randomUUID().toString()
    private val headers = ConcurrentHashMap<String, String>()
    private val masterUrl = AtomicReference("")
    private val generation = AtomicLong(0L)
    private val expiry = AtomicLong(0L)
    private val view = AtomicReference<WebView?>(null)
    private val refreshAction = AtomicReference<(() -> Unit)?>(null)
    private val releaseAction = AtomicReference<((WebView) -> Unit)?>(null)
    private val audioTracks = AtomicReference<List<AllohaParsedAudioTrack>>(emptyList())
    private val subtitles = AtomicReference<List<AllohaSubtitleTrack>>(emptyList())
    private val selectedAudioId = AtomicReference<String?>(null)
    private val selectedQuality = AtomicReference<String?>(null)
    private val proxy = AtomicReference<AllohaStreamProxy?>(null)
    private val streamStateLock = Any()

    /**
     * Shadow slot for a session rotation in progress. A WebView reload re-emits its signals in
     * stages - onReady first (carrying the raw bnsi URL the CDN answers with 403), then the
     * correctly signed master and the fresh edge_hash a second or more later. Writing those
     * straight into the live state would leave the proxy serving a half-updated, unauthorized
     * session for that whole window, which is exactly the 2-3s stall Media3 shows as buffering.
     * Instead every update during a rotation lands here and is committed atomically once all
     * three signals arrived, so the still-valid previous token keeps serving until then.
     */
    private class StagedState {
        val headers = mutableMapOf<String, String>()
        var masterUrl: String? = null
        var expiry: Long = 0L
        var sources: AllohaParsedSources? = null
        var hasReady = false
        var hasRefreshedMaster = false
        var hasConfigUpdate = false

        fun isComplete(requireConfigUpdate: Boolean): Boolean =
            hasReady && hasRefreshedMaster && (!requireConfigUpdate || hasConfigUpdate)
    }

    // Guarded by streamStateLock.
    private var staging: StagedState? = null

    /**
     * True once a real `config_update` has reached this session. Measured on device: with some CDNs
     * the player's WebSocket is torn down ~2.5s after it opens - before the 25s heartbeat can fire -
     * and no config_update ever arrives. A rotation that insists on one can then only ever end at
     * [STAGED_COMMIT_TIMEOUT_MS], so every single rotation pays the full 8s stall. When this session
     * has never seen one, the rotation commits on ready + master alone.
     *
     * Guarded by [streamStateLock].
     */
    private var sawConfigUpdate = false
    private val stagedCommitTimeout = Runnable {
        synchronized(streamStateLock) {
            if (staging == null) return@Runnable
            analytics.log(LOG_TAG) {
                "staged session commit timed out, applying what arrived " +
                        "ready=${staging?.hasReady} master=${staging?.hasRefreshedMaster} " +
                        "config=${staging?.hasConfigUpdate}"
            }
            commitStagedLocked()
        }
    }

    /** The dubbing currently selected, falling back to Alloha's own default. */
    private fun currentTrack(): AllohaParsedAudioTrack? {
        val tracks = audioTracks.get()
        val id = selectedAudioId.get()
        return tracks.firstOrNull { it.track.id == id }
            ?: tracks.firstOrNull { it.track.isDefault }
            ?: tracks.firstOrNull()
    }

    /** Resolves the real CDN master for a (dubbing, quality) pair - the proxy's lookup. */
    private fun masterFor(audioId: String?, quality: String?): String? {
        val tracks = audioTracks.get()
        val track = tracks.firstOrNull { it.track.id == audioId } ?: currentTrack() ?: return null
        val label = quality ?: selectedQuality.get()
        return label?.let(track.qualities::get) ?: track.qualities.values.lastOrNull()
    }

    override val initialStream: PlayerStreamResolveResult.Stream
        get() {
            val activeProxy = checkNotNull(proxy.get())
            val track = checkNotNull(currentTrack())
            return PlayerStreamResolveResult.Stream(
                url = activeProxy.streamUrl(track.track.id, selectedQuality.get()),
                headers = emptyMap(),
                qualities = track.qualities.keys.associateTo(linkedMapOf()) { label ->
                    label to activeProxy.streamUrl(track.track.id, label)
                },
                qualityHeaders = emptyMap(),
                allohaAudioTracks = audioTracks.get().map(AllohaParsedAudioTrack::track),
                selectedAllohaAudioId = track.track.id,
                // Subtitles ride the same loopback proxy so the CDN sees this session's headers.
                allohaSubtitles = subtitles.get()
                    .map { it.copy(url = activeProxy.sideloadUrl(it.url)) },
            )
        }

    /** Direct CDN stream (no proxy) - used by the one-shot extract path, e.g. downloads. */
    val directStream: PlayerStreamResolveResult.Stream
        get() {
            val track = checkNotNull(currentTrack())
            val currentHeaders = currentHeaders()
            val label = selectedQuality.get()
            return PlayerStreamResolveResult.Stream(
                url = label?.let(track.qualities::get) ?: track.qualities.values.last(),
                headers = currentHeaders,
                qualities = track.qualities,
                qualityHeaders = track.qualities.keys.associateWith { currentHeaders },
                allohaAudioTracks = audioTracks.get().map(AllohaParsedAudioTrack::track),
                selectedAllohaAudioId = track.track.id,
                allohaSubtitles = subtitles.get(),
            )
        }

    override val playbackUrl: String
        get() = checkNotNull(proxy.get()).streamUrl(
            currentTrack()?.track?.id,
            selectedQuality.get()
        )

    override val qualityUrls: LinkedHashMap<String, String>
        get() {
            val activeProxy = checkNotNull(proxy.get())
            val track = currentTrack() ?: return linkedMapOf()
            return track.qualities.keys.associateTo(linkedMapOf()) { label ->
                label to activeProxy.streamUrl(track.track.id, label)
            }
        }

    fun attach(webView: WebView, refresh: () -> Unit, release: (WebView) -> Unit) {
        view.set(webView)
        refreshAction.set(refresh)
        releaseAction.set(release)
    }

    fun initialize(sources: AllohaParsedSources, streamHeaders: Map<String, String>) {
        synchronized(streamStateLock) {
            val staged = staging
            if (staged != null) {
                staged.sources = sources
                staged.masterUrl =
                    sources.audioTracks.firstOrNull()?.qualities?.values?.lastOrNull()
                staged.headers.clear()
                staged.headers.putAll(streamHeaders.mapKeys { it.key.lowercase() })
                staged.hasReady = true
                commitStagedIfReadyLocked()
                return
            }
            applySourcesLocked(sources)
            masterUrl.set(masterFor(selectedAudioId.get(), selectedQuality.get()).orEmpty())
            headers.clear()
            headers.putAll(streamHeaders.mapKeys { it.key.lowercase() })
            generation.incrementAndGet()
        }
    }

    /** Applies a freshly parsed source set, carrying the user's dubbing/quality choice across. */
    private fun applySourcesLocked(sources: AllohaParsedSources) {
        audioTracks.set(sources.audioTracks)
        subtitles.set(sources.subtitles)
        val previousAudio = selectedAudioId.get()
        selectedAudioId.set(
            previousAudio?.takeIf { id -> sources.audioTracks.any { it.track.id == id } }
                ?: sources.audioTracks.firstOrNull { it.track.isDefault }?.track?.id
                ?: sources.audioTracks.firstOrNull()?.track?.id
        )
        val available = currentTrack()?.qualities.orEmpty()
        selectedQuality.set(selectedQuality.get()?.takeIf(available::containsKey))
    }

    /** Picks the preferred quality once, right after the first parse. */
    fun preselectQuality(label: String?) {
        val available = currentTrack()?.qualities.orEmpty()
        if (label != null && available.containsKey(label)) selectedQuality.set(label)
    }

    fun startProxy() {
        if (proxy.get() != null) return
        analytics.log(LOG_TAG) { "starting localhost proxy ${safeHeaderState()}" }
        proxy.compareAndSet(
            null,
            AllohaStreamProxy(
                streamStateProvider = ::currentStreamState,
                masterProvider = ::masterFor,
                requestSessionRefresh = ::refresh,
                analytics = analytics,
            )
        )
    }

    fun updateHeaders(value: Map<String, String>) {
        synchronized(streamStateLock) {
            val staged = staging
            if (staged != null) {
                staged.headers.putAll(value.mapKeys { it.key.lowercase() })
                commitStagedIfReadyLocked()
            } else {
                headers.putAll(value.mapKeys { it.key.lowercase() })
            }
        }
    }

    fun updateMasterUrl(value: String) {
        synchronized(streamStateLock) {
            if (value.isBlank()) return
            val staged = staging
            if (staged != null) {
                staged.masterUrl = value
                staged.hasRefreshedMaster = true
                commitStagedIfReadyLocked()
            } else {
                masterUrl.set(value)
            }
        }
    }

    fun updateExpiry(ttlSeconds: Int) = applyExpiry(ttlSeconds, fromConfigUpdate = true)

    fun ensureFallbackExpiry(ttlSeconds: Int) {
        synchronized(streamStateLock) {
            val current = staging?.expiry ?: expiry.get()
            // A guessed TTL must not count as the config_update a staged rotation waits for:
            // only a real one carries the edge_hash the new master is signed with.
            if (current <= System.currentTimeMillis()) {
                applyExpiry(ttlSeconds, fromConfigUpdate = false)
            }
        }
    }

    private fun applyExpiry(ttlSeconds: Int, fromConfigUpdate: Boolean) {
        synchronized(streamStateLock) {
            if (fromConfigUpdate) sawConfigUpdate = true
            val value = System.currentTimeMillis() + ttlSeconds * 1_000L
            val staged = staging
            if (staged != null) {
                staged.expiry = value
                if (fromConfigUpdate) {
                    staged.hasConfigUpdate = true
                    commitStagedIfReadyLocked()
                }
            } else {
                expiry.set(value)
            }
        }
    }

    private fun commitStagedIfReadyLocked() {
        if (staging?.isComplete(requireConfigUpdate = sawConfigUpdate) == true) commitStagedLocked()
    }

    /** Swaps the staged rotation into the live state in one step. Call under [streamStateLock]. */
    private fun commitStagedLocked() {
        val staged = staging ?: return
        staging = null
        handler.removeCallbacks(stagedCommitTimeout)

        // Carry the selection across by id/label: the rotated session serves the same dubbing and
        // quality ladder behind fresh URLs, so matching on URL would lose the choice.
        staged.sources?.let(::applySourcesLocked)
        staged.masterUrl?.takeIf(String::isNotBlank)?.let(masterUrl::set)
        if (staged.expiry > 0L) expiry.set(staged.expiry)
        if (staged.headers.isNotEmpty()) {
            headers.clear()
            headers.putAll(staged.headers)
        }
        generation.incrementAndGet()
        analytics.log(LOG_TAG) { "staged session committed ${safeHeaderStateLocked()}" }
    }

    override fun currentHeaders(): Map<String, String> = synchronized(streamStateLock) {
        headers.toMap()
    }

    private fun currentStreamState(): AllohaStreamState = synchronized(streamStateLock) {
        AllohaStreamState(
            headers = headers.toMap(),
            masterUrl = masterUrl.get(),
            generation = generation.get(),
            expiresAtMs = expiry.get().takeIf { it > 0L },
        )
    }

    fun safeHeaderState(): String = synchronized(streamStateLock) { safeHeaderStateLocked() }

    private fun safeHeaderStateLocked(): String {
        val host = runCatching { URL(masterUrl.get()).host }.getOrDefault("unknown")
        val ttlSeconds = expiry.get().takeIf { it > 0L }
            ?.let { ((it - System.currentTimeMillis()) / 1_000L).coerceAtLeast(0L) }
        val cookie = sequenceOf(masterUrl.get(), headers["origin"], headers["referer"])
            .filterNotNull()
            .filter(String::isNotBlank)
            .mapNotNull {
                runCatching { CookieManager.getInstance().getCookie(it) }.getOrNull()
            }
            .firstOrNull(String::isNotBlank)
        return "generation=${generation.get()} host=$host ttl=${ttlSeconds ?: "none"} " +
                "names=${headers.keys.sorted()} " +
                "auth=${safeFingerprint(headers["authorizations"])} " +
                "controls=${safeFingerprint(headers["accepts-controls"])} " +
                "ua=${safeFingerprint(headers["user-agent"])} " +
                "cookie=${safeFingerprint(cookie)}"
    }

    override fun currentMasterUrl(): String = synchronized(streamStateLock) { masterUrl.get() }

    /** True while a rotation is staged and has not been committed to the live state yet. */
    val isRotating: Boolean get() = synchronized(streamStateLock) { staging != null }

    /** See [sawConfigUpdate] - false means no `config_update` can be expected for this session. */
    val hasSeenConfigUpdate: Boolean get() = synchronized(streamStateLock) { sawConfigUpdate }

    override fun expiresAtMs(): Long? = expiry.get().takeIf { it > 0L }
    override fun refresh() {
        // The live headers/master/expiry stay untouched: the current token is still valid for
        // SESSION_REFRESH_LEAD_MS and must keep serving segments while the reloaded WebView
        // assembles the next one. Everything the reload emits lands in `staging` instead and is
        // swapped in atomically by commitStagedLocked().
        synchronized(streamStateLock) {
            if (staging != null) return
            staging = StagedState()
        }
        handler.post {
            handler.removeCallbacks(stagedCommitTimeout)
            handler.postDelayed(stagedCommitTimeout, STAGED_COMMIT_TIMEOUT_MS)
            refreshAction.get()?.invoke()
        }
    }

    override fun selectQuality(label: String) {
        if (currentTrack()?.qualities?.containsKey(label) == true) selectedQuality.set(label)
    }

    override fun selectAudioTrack(id: String) {
        synchronized(streamStateLock) {
            if (audioTracks.get().none { it.track.id == id }) return
            selectedAudioId.set(id)
            // Keep the quality choice only if the new dubbing actually offers it.
            val available = currentTrack()?.qualities.orEmpty()
            selectedQuality.set(selectedQuality.get()?.takeIf(available::containsKey))
            masterFor(id, selectedQuality.get())?.let(masterUrl::set)
            analytics.log(LOG_TAG) { "audio track selected id=$id" }
        }
    }

    override fun close() {
        handler.removeCallbacks(stagedCommitTimeout)
        synchronized(streamStateLock) { staging = null }
        handler.post {
            proxy.getAndSet(null)?.close()
            refreshAction.set(null)
            val release = releaseAction.getAndSet(null)
            view.getAndSet(null)?.let {
                it.removeJavascriptInterface(BRIDGE_NAME)
                it.stopLoading()
                if (release != null) {
                    // No loadUrl("about:blank") before pooling: that navigation is asynchronous
                    // while the instance returns to the pool immediately, so it can still be in
                    // flight when the next session loads its wrapper into the same WebView.
                    // onPause() stops an idle instance from decoding without queueing anything;
                    // the next loadDataWithBaseURL replaces the content, and AllohaExtractor calls
                    // onResume()/resumeTimers() on acquire.
                    it.onPause()
                    release(it)
                } else {
                    it.destroy()
                }
            }
        }
    }
}

private fun safeFingerprint(value: String?): String {
    if (value.isNullOrBlank()) return "none"
    return MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .take(4)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
