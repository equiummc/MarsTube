#!/usr/bin/env python3
"""MT-010A: opt-in short-lived per-player n-result memoization (isolated build).

Apply AFTER MT-008 and MT-009 probes on pinned MediaServiceCore.
Only reuse an EXACT (player URL, n input) pair; never reuse across player revisions.
No account IDs, tokens or decoded inputs printed or persisted.
Fail closed when upstream anchors change. --check validates without editing.
"""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / "MediaServiceCore"
PIN = "40fec843eb24bc264e9c8b429fcee25068e230e2"
check_only = "--check" in sys.argv[1:]
sha = subprocess.check_output(["git", "-C", str(CORE), "rev-parse", "HEAD"], text=True).strip()
if sha != PIN:
    raise SystemExit("MT-010A STOP: unexpected MediaServiceCore SHA")

source = CORE / "youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/nsigsolver/impl/TclChallengeProvider.kt"
original = source.read_text(encoding="utf-8")
updated = original

def replace_one(label, old, new):
    global updated
    matches = updated.count(old)
    if matches != 1:
        raise SystemExit(f"MT-010A STOP: {label}: expected exactly one anchor, found {matches}")
    updated = updated.replace(old, new)

replace_one("cache fields",
    """    private const val cacheSection = "tcl-challenge-solver"

    private data class SolverOutput""",
    """    private const val cacheSection = "tcl-challenge-solver"

    // MT-010A: per-process replay of verified, previously SOLVED n challenges.
    // Inputs are scoped to the exact player URL and expire after 90 seconds.
    // Cap memory at 64 entries. Data never reaches logs or persistent storage.
    private const val decodedTtlMs = 90_000L
    private const val decodedCapacity = 64
    private data class DecodedN(val output: String, val atMs: Long)
    private val decodedLock = Any()
    private val decoded = LinkedHashMap<String, DecodedN>(64, 0.75f, true)

    private data class SolverOutput""")
replace_one("solver lookups",
    """        // n-params are opaque URL-safe tokens; wrap each in a throwaway url so the TCL
        // n-function (which expects a real URL object, not a bare string) has something valid
        // to parse. The wrapper's own shape doesn't matter beyond being a well-formed https url.
        val challengeByParam = nParams.distinct().associateWith { "https://www.youtube.com/videoplayback?n=$it" }""",
    """        // MT-010A: ONLY identical exact input/player pairs may be reused.
        // All misses take the unchanged upstream V8 solver path.
        val nowMs = android.os.SystemClock.elapsedRealtime()
        val hits = HashMap<String, String>()
        val missed = ArrayList<String>()
        synchronized(decodedLock) {
            for (param in nParams.distinct()) {
                val entry = decoded[playerUrl + "\\u0000" + param]
                if (entry != null && nowMs >= entry.atMs &&
                    nowMs - entry.atMs < decodedTtlMs) {
                    hits[param] = entry.output
                } else {
                    missed.add(param)
                    decoded.remove(playerUrl + "\\u0000" + param)
                }
            }
        }
        android.util.Log.i("MarsTubePerf", "MT-010A decoder_hits=" + hits.size +
            " decoder_misses=" + missed.size)
        if (missed.isEmpty()) {
            return nParams.associateWith { hits[it] }
        }

        // The unmodified JS algorithm processes only the unsolved subset.
        val challengeByParam = missed.associateWith { "https://www.youtube.com/videoplayback?n=$it" }""")
replace_one("returned values",
    """        val results = output.results ?: emptyMap()

        return nParams.associateWith { param -> results[challengeByParam[param]] }""",
    """        val results = output.results ?: emptyMap()
        val newlySolved = missed.associateWith { param -> results[challengeByParam[param]] }
        synchronized(decodedLock) {
            val storedAt = android.os.SystemClock.elapsedRealtime()
            for ((param, value) in newlySolved) {
                if (value != null && value.isNotEmpty()) {
                    decoded[playerUrl + "\\u0000" + param] = DecodedN(value, storedAt)
                }
            }
            while (decoded.size > decodedCapacity) {
                val oldest = decoded.keys.iterator()
                if (oldest.hasNext()) {
                    oldest.next()
                    oldest.remove()
                }
            }
        }
        return nParams.associateWith { param -> hits[param] ?: newlySolved[param] }""")

# An existing Android CI build exercises Kotlin compile. The cache itself may
# have zero hits on new Shorts; never claim an improvement without TV timings.
if not check_only:
    source.write_text(updated, encoding="utf-8", newline="\n")
print("MT-010A " + ("anchors validated" if check_only else "experimental decoder cache instrumented"))
