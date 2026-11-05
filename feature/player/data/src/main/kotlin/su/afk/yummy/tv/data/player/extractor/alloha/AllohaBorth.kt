package su.afk.yummy.tv.data.player.extractor.alloha

/**
 * Builds the `Borth` request header for `bnsi`: `<fingerprint>|<permuted viewporti>`. The fingerprint is the
 * install's own ([allohaFingerprint]): a random 64-hex is refused with `client_blocked`, a hash built the way
 * the page builds it is accepted.
 *
 * `viewporti` is the per-page-load seed from `<meta name="viewporti">`. The player script does not
 * encrypt it, it only shuffles its characters, so the server can tell a client that ran the page
 * code from one that merely replayed a URL. The shuffle must be reproduced exactly.
 */
internal fun allohaBorthHeader(fingerprint: String, viewporti: String): String =
    "$fingerprint|${allohaBorthTail(viewporti)}"

/** The seed after the page's three permutation passes. */
internal fun allohaBorthTail(seed: String): String =
    unscramble(reorderByBitWidth(reorderByBitDepth(seed)))

private fun ceilLog2(length: Int): Int {
    var u = 0
    while ((1 shl u) < length) u++
    return u
}

/** Regroups characters by the bit length of their index; groups are laid out largest-first. */
private fun reorderByBitDepth(source: String): String {
    val length = source.length
    if (length <= 0) return source
    val u = ceilLog2(length)

    fun bucketIndex(index: Int): Int = if (index == 0) 0 else Int.SIZE_BITS - Integer.numberOfLeadingZeros(index)

    val sizes = IntArray(u + 1)
    for (index in 0 until length) sizes[bucketIndex(index)]++

    val buckets = Array(u + 1) { "" }
    var pos = 0
    for (i in u downTo 0) {
        buckets[i] = source.substring(pos, pos + sizes[i])
        pos += sizes[i]
    }
    return takeFromBuckets(length, u, buckets, ::bucketIndex)
}

/** Regroups characters by the number of trailing zero bits of their index (index 0 is the top group). */
private fun reorderByBitWidth(source: String): String {
    val length = source.length
    if (length <= 0) return source
    val u = ceilLog2(length)

    fun bucketIndex(index: Int): Int = if (index == 0) u else Integer.numberOfTrailingZeros(index)

    val sizes = IntArray(u + 1)
    for (index in 0 until length) sizes[bucketIndex(index)]++

    val buckets = Array(u + 1) { "" }
    var pos = 0
    for (i in 0..u) {
        buckets[i] = source.substring(pos, pos + sizes[i])
        pos += sizes[i]
    }
    return takeFromBuckets(length, u, buckets, ::bucketIndex)
}

private fun takeFromBuckets(
    length: Int,
    maxBucket: Int,
    buckets: Array<String>,
    bucketIndex: (Int) -> Int,
): String {
    val pointers = IntArray(maxBucket + 1)
    val result = CharArray(length)
    for (index in 0 until length) {
        val bucket = bucketIndex(index)
        result[index] = buckets[bucket][pointers[bucket]++]
    }
    return String(result)
}

/** Places characters by walking indices in steps of 2 modulo the first prime above `length`. */
private fun unscramble(source: String): String {
    val length = source.length
    if (length <= 1) return source

    val modulus = nextPrimeAtOrAbove(length + 1)
    val visited = BooleanArray(length)
    val order = IntArray(length)
    var filled = 0
    var current = 0
    while (filled < length) {
        current = (current + 2) % modulus
        if (current < length && !visited[current]) {
            order[filled++] = current
            visited[current] = true
        }
    }

    val decoded = CharArray(length)
    for (index in 0 until length) decoded[order[index]] = source[index]
    return String(decoded)
}

private fun isPrime(n: Int): Boolean {
    if (n < 2) return false
    if (n % 2 == 0) return n == 2
    var i = 3
    while (i * i <= n) {
        if (n % i == 0) return false
        i += 2
    }
    return true
}

private fun nextPrimeAtOrAbove(value: Int): Int {
    var candidate = maxOf(1, value)
    while (!isPrime(candidate)) candidate++
    return candidate
}
