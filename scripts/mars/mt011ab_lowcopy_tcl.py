#!/usr/bin/env python3
"""MT-011AB: bypass repeated large TCL JSON serialization on warm V8 sessions.

Apply after MT-008, MT-009, MT-010A, MT-010A2, MT-011A diagnostic overlays.
Keeps existing player source matching and challenge semantics. The warm solver
is only invoked on the dedicated V8 worker when its last installed source
matches the whole exact cached preprocessed source. Failure remains fail-closed.
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
    raise SystemExit("MT-011AB STOP: unexpected MediaServiceCore revision")

def patch(relative, steps):
    path = CORE / relative
    updated = path.read_text(encoding="utf-8")
    for label, old, new in steps:
        found = updated.count(old)
        if found != 1:
            raise SystemExit(f"MT-011AB STOP: {relative} {label}: expected 1, got {found}")
        updated = updated.replace(old, new)
    if not CHECK:
        path.write_text(updated, encoding="utf-8", newline="\n")
    print("MT-011AB " + ("checked " if CHECK else "patched ") + relative)

patch("youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/nsigsolver/impl/TclChallengeProvider.kt", [
    ("worker-owned exact script identity",
     """    private const val idleLifetimeMs = 45_000L""",
     """    private const val idleLifetimeMs = 45_000L
    // Read and written ONLY on the dedicated TCL V8 HandlerThread. Never use
    // a script from a different revision or after V8 was released.
    private var mt011LastPreparedSource: String? = null"""),
    ("skip bulk JSON encoding off worker for cache hits",
     """        val mt011EncodedRequest = Gson().toJson(request)""",
     """        // Large cached player scripts took ~268ms to re-encode on Thomson.
        // Defer the one unavoidable full encode to the V8 worker only on a
        // genuine script miss. Each playback always encodes fresh challenges.
        val mt011EncodedRequest =
            if (cachedPreprocessed == null) Gson().toJson(request) else ""
        val mt011ChallengeJson = Gson().toJson(challengeByParam.values.toList())"""),
    ("skip large JS string off worker for cache hits",
     """        val stdin = "JSON.stringify(tclSolver($mt011EncodedRequest));" """.rstrip(),
     """        val stdin = if (cachedPreprocessed == null)
            "JSON.stringify(tclSolver($mt011EncodedRequest));" else """""),
    ("execute exact cached JS or full request",
     """                        val result = runV8(stdin)
                        android.util.Log.i("MarsTubePerf", "MT-009 tcl_v8_exec_ms=" +""",
     """                        val mt011PrepareStartNs = android.os.SystemClock.elapsedRealtimeNanos()
                        val mt011Reuse = warm && cachedPreprocessed != null &&
                            mt011LastPreparedSource == cachedPreprocessed
                        val mt011Command = if (mt011Reuse) {
                            // Function was compiled for precisely these source bytes
                            // on this same still-live V8 instance.
                            "JSON.stringify(tclSolver({\"type\":\"reuse\",\"challenges\":$mt011ChallengeJson}));"
                        } else if (cachedPreprocessed != null) {
                            "JSON.stringify(tclSolver(${Gson().toJson(request)}));"
                        } else {
                            stdin
                        }
                        android.util.Log.i("MarsTubePerf",
                            "MT-011AB tcl_script_reuse=" + mt011Reuse +
                            " tcl_invocation_build_us=" +
                            ((android.os.SystemClock.elapsedRealtimeNanos() - mt011PrepareStartNs) / 1000L))
                        val result = runV8(mt011Command)
                        // A failed V8 solve is not considered a proven compiled
                        // function; a future call must take the original path.
                        mt011LastPreparedSource =
                            if (cachedPreprocessed != null && result.startsWith("{\"type\":\"result\""))
                                cachedPreprocessed else null
                        android.util.Log.i("MarsTubePerf", "MT-009 tcl_v8_exec_ms=" +"""),
    ("clear source identity when V8 released",
     """            shutdownIfNeeded()
            android.util.Log.i("MarsTubePerf", "MT-010A2 v8_idle_release_ms=" +""",
     """            shutdownIfNeeded()
            mt011LastPreparedSource = null
            android.util.Log.i("MarsTubePerf", "MT-010A2 v8_idle_release_ms=" +""")
])
patch("youtubeapi/src/main/assets/nsigsolver/tcl.solver.js", [
    ("reuse exact compiled function only after worker Kotlin validation",
     """  function main(input) {
    try {
      const preprocessedPlayer = input.type === "preprocessed" """.rstrip(),
     """  function main(input) {
    try {
      // MT-011AB: native worker validates the FULL script identity before
      // omitting it from an invocation. Never guess the version in JavaScript.
      if (input.type === "reuse") {
        if (mt010CompiledFunction === null) throw new Error("No warm TCL function");
        const results = {};
        for (const challenge of input.challenges) {
          results[challenge] = mt010CompiledFunction(challenge);
        }
        return { type: "result", results: results };
      }
      const preprocessedPlayer = input.type === "preprocessed" """.rstrip())
])
print("MT-011AB: no player/stream changes; short challenge-only worker calls on exact warm script")
