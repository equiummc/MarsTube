#!/usr/bin/env python3
"""MT-011AB2: one-entry bounded RAM cache for the unchanged TCL preprocess source.

Apply after MT-011AB low-copy overlay, not to the pinned upstream gitlink.
Disk cache remains authoritative on a miss. The exact player URL is the key,
TTL is no longer than V8's existing 45s idle release. Invalidate with upstream.
"""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / "MediaServiceCore"
PIN = "40fec843eb24bc264e9c8b429fcee25068e230e2"
CHECK = "--check" in sys.argv[1:]
if subprocess.check_output(["git", "-C", str(CORE), "rev-parse", "HEAD"], text=True).strip() != PIN:
    raise SystemExit("MT-011AB2 STOP: unexpected submodule SHA")

p = CORE / "youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/nsigsolver/impl/TclChallengeProvider.kt"
s = p.read_text(encoding="utf-8")
steps = [
    ("cache state",
     """    private const val idleLifetimeMs = 45_000L""",
     """    // At most one exact player URL/source, limited to ~1MiB UTF-16.
    // This mirrors the existing persisted TCL preprocess cache, not media.
    private val mt011CacheLock = Any()
    private const val mt011MemoryTtlMs = 45_000L
    private const val mt011MaxChars = 512_000
    private var mt011MemoryKey: String? = null
    private var mt011MemoryCode: String? = null
    private var mt011MemoryAtMs = 0L
    private const val idleLifetimeMs = 45_000L"""),
    ("lookup shortcut",
     """        val cachedPreprocessed = CacheService.load(cacheSection, cacheKey)?.code""",
     """        val mt011NowMs = android.os.SystemClock.elapsedRealtime()
        var mt011MemoryHit = false
        val cachedPreprocessed = synchronized(mt011CacheLock) {
            if (mt011MemoryKey == cacheKey && mt011MemoryCode != null &&
                mt011NowMs >= mt011MemoryAtMs &&
                mt011NowMs - mt011MemoryAtMs < mt011MemoryTtlMs) {
                mt011MemoryHit = true
                mt011MemoryCode
            } else {
                val disk = CacheService.load(cacheSection, cacheKey)?.code
                mt011MemoryKey = if (disk != null && disk.length <= mt011MaxChars) cacheKey else null
                mt011MemoryCode = if (disk != null && disk.length <= mt011MaxChars) disk else null
                mt011MemoryAtMs = mt011NowMs
                disk
            }
        }
        android.util.Log.i("MarsTubePerf", "MT-011AB tcl_memory_cache_hit=" + mt011MemoryHit)"""),
    ("stale invalidation",
     """                CacheService.clear(cacheSection)""",
     """                CacheService.clear(cacheSection)
                synchronized(mt011CacheLock) {
                    mt011MemoryKey = null
                    mt011MemoryCode = null
                    mt011MemoryAtMs = 0L
                }"""),
    ("preprocessed store",
     """        output.preprocessed_player?.let { CacheService.store(cacheSection, cacheKey, CachedData(it)) }""",
     """        output.preprocessed_player?.let { prepared ->
            CacheService.store(cacheSection, cacheKey, CachedData(prepared))
            synchronized(mt011CacheLock) {
                mt011MemoryKey = if (prepared.length <= mt011MaxChars) cacheKey else null
                mt011MemoryCode = if (prepared.length <= mt011MaxChars) prepared else null
                mt011MemoryAtMs = android.os.SystemClock.elapsedRealtime()
            }
        }""")
]
for label, old, new in steps:
    count = s.count(old)
    if count != 1:
        raise SystemExit(f"MT-011AB2 STOP: {label} anchor count={count}")
    s = s.replace(old, new)
if not CHECK:
    p.write_text(s, encoding="utf-8", newline="\n")
print("MT-011AB2 " + ("anchors checked" if CHECK else "bounded RAM TCL source cache applied"))
