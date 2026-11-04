package su.afk.yummy.tv.domain.player.model

/**
 * One dubbing/voice-over Alloha offers for the current episode, taken from the `hlsSource` array of
 * its own `bnsi` response - the same list its player shows behind the gear icon. Each entry carries
 * its own full quality ladder, so switching dubbing is a stream swap from state (like quality), not
 * a new extraction.
 *
 * @param qualities this dubbing's ladder: label to CDN URL, ascending by height. All dubbings share
 * the stream's headers.
 */
data class AllohaAudioTrack(
    val id: String,
    val label: String,
    val isDefault: Boolean = false,
    val qualities: LinkedHashMap<String, String> = LinkedHashMap(),
)
