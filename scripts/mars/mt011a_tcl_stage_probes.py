#!/usr/bin/env python3
"""MT-011A1: isolate the ~300ms TCL prepare-to-V8 gap without changing output.

Apply ONLY after MT-008, MT-009, MT-010A1, MT-010A2 overlays in an
isolated checkout of pinned MediaServiceCore. --check is read-only.
Emits durations ONLY: no player URLs, signatures, challenge strings, tokens
or raw JavaScript. Retains the 45s V8 idle policy and solver behavior.
"""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / "MediaServiceCore"
PIN = "40fec843eb24bc264e9c8b429fcee25068e230e2"
CHECK = "--check" in sys.argv[1:]
actual = subprocess.check_output(
    ["git", "-C", str(CORE), "rev-parse", "HEAD"], text=True
).strip()
if actual != PIN:
    raise SystemExit("MT-011A STOP: unrecognized MediaServiceCore SHA")

target = CORE / "youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/nsigsolver/impl/TclChallengeProvider.kt"
source = target.read_text(encoding="utf-8")
steps = [
    (
        "request build begin",
        "        val request = if (cachedPreprocessed != null) {",
        """        val mt011RequestStartNs = android.os.SystemClock.elapsedRealtimeNanos()
        val request = if (cachedPreprocessed != null) {"""
    ),
    (
        "Gson stage split",
        """        val stdin = "JSON.stringify(tclSolver(${Gson().toJson(request)}));" """.rstrip(),
        """        android.util.Log.i("MarsTubePerf", "MT-011A tcl_request_build_us=" +
            ((android.os.SystemClock.elapsedRealtimeNanos() - mt011RequestStartNs) / 1000L))
        val mt011EncodeStartNs = android.os.SystemClock.elapsedRealtimeNanos()
        val mt011EncodedRequest = Gson().toJson(request)
        android.util.Log.i("MarsTubePerf", "MT-011A tcl_json_encode_us=" +
            ((android.os.SystemClock.elapsedRealtimeNanos() - mt011EncodeStartNs) / 1000L))
        val mt011StdinStartNs = android.os.SystemClock.elapsedRealtimeNanos()
        val stdin = "JSON.stringify(tclSolver($mt011EncodedRequest));"
        android.util.Log.i("MarsTubePerf", "MT-011A tcl_stdin_build_us=" +
            ((android.os.SystemClock.elapsedRealtimeNanos() - mt011StdinStartNs) / 1000L))"""
    ),
    (
        "worker queue clock",
        """            val task = java.util.concurrent.FutureTask<String> {
                synchronized(v8Lock) {""",
        """            val mt011SubmittedNs = android.os.SystemClock.elapsedRealtimeNanos()
            val task = java.util.concurrent.FutureTask<String> {
                val mt011WorkerStartNs = android.os.SystemClock.elapsedRealtimeNanos()
                android.util.Log.i("MarsTubePerf", "MT-011A tcl_worker_queue_us=" +
                    ((mt011WorkerStartNs - mt011SubmittedNs) / 1000L))
                val mt011LockStartNs = android.os.SystemClock.elapsedRealtimeNanos()
                synchronized(v8Lock) {
                    android.util.Log.i("MarsTubePerf", "MT-011A tcl_worker_lock_us=" +
                        ((android.os.SystemClock.elapsedRealtimeNanos() - mt011LockStartNs) / 1000L))"""
    ),
    (
        "worker setup",
        """                    tclHandler.removeCallbacks(releaseIdleRuntime)
                    val warm = v8Runtime != null""",
        """                    val mt011SetupStartNs = android.os.SystemClock.elapsedRealtimeNanos()
                    tclHandler.removeCallbacks(releaseIdleRuntime)
                    val warm = v8Runtime != null
                    android.util.Log.i("MarsTubePerf", "MT-011A tcl_worker_setup_us=" +
                        ((android.os.SystemClock.elapsedRealtimeNanos() - mt011SetupStartNs) / 1000L))"""
    ),
]
for label, old, new in steps:
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"MT-011A STOP: {label}: expected 1 anchor, got {count}")
    source = source.replace(old, new)
if not CHECK:
    target.write_text(source, encoding="utf-8", newline="\n")
print("MT-011A " + ("source anchors verified" if CHECK else "TCL stage diagnostics injected"))
