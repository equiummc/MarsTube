#!/usr/bin/env python3
"""MT-010A2: experimental dedicated TCL V8 worker + compiled-function reuse.

Apply after MT-008 -> MT-009 -> MT-010A1 in separate checkout.
Verifies pinned MediaServiceCore and exact overlay anchors, --check only checks.
The TCL V8 engine is accessed exclusively from a single HandlerThread and
released ON THAT SAME THREAD after 45 seconds idle. Only one compiled script
version is cached within the JS engine; input n parameters never cached by JS.
Never use on a production device without A/B real output + memory validation.
"""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / "MediaServiceCore"
PIN = "40fec843eb24bc264e9c8b429fcee25068e230e2"
if subprocess.check_output(["git", "-C", str(CORE), "rev-parse", "HEAD"], text=True).strip() != PIN:
    raise SystemExit("MT-010A2 STOP: unexpected MediaServiceCore revision")
check = "--check" in sys.argv[1:]

def patch(relative, steps):
    p = CORE / relative
    s = p.read_text(encoding="utf-8")
    for label, old, new in steps:
        if s.count(old) != 1:
            raise SystemExit(f"MT-010A2 STOP: {relative} anchor {label}: {s.count(old)}")
        s = s.replace(old, new)
    if not check:
        p.write_text(s, encoding="utf-8", newline="\n")
    print(f"MT-010A2 {('checked' if check else 'instrumented')}: {relative}")

patch("youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/nsigsolver/impl/TclChallengeProvider.kt", [
    ("worker state",
     """    private val v8Lock = Any()
    private const val cacheSection = "tcl-challenge-solver" """.rstrip(),
     """    private val v8Lock = Any()
    // J2V8 runtime must be created, used and released on the same worker
    // thread, never leaked to independent Rx workers.
    private val tclWorker by lazy {
        android.os.HandlerThread("MarsTube-TCL-V8").apply { start() }
    }
    private val tclHandler by lazy { android.os.Handler(tclWorker.looper) }
    private val releaseIdleRuntime = Runnable {
        synchronized(v8Lock) {
            val mt010Start = android.os.SystemClock.elapsedRealtime()
            shutdownIfNeeded()
            android.util.Log.i("MarsTubePerf", "MT-010A2 v8_idle_release_ms=" +
                (android.os.SystemClock.elapsedRealtime() - mt010Start))
        }
    }
    private const val idleLifetimeMs = 45_000L
    private const val cacheSection = "tcl-challenge-solver" """.rstrip()),
    ("V8 operation",
     """        val stdout = try {
            synchronized(v8Lock) {
                val mt009Init = android.os.SystemClock.elapsedRealtime()
                initRuntime()
                android.util.Log.i("MarsTubePerf", "MT-009 tcl_v8_init_ms=" +
                    (android.os.SystemClock.elapsedRealtime() - mt009Init))
                val mt009Exec = android.os.SystemClock.elapsedRealtime()
                val result = runV8(stdin)
                android.util.Log.i("MarsTubePerf", "MT-009 tcl_v8_exec_ms=" +
                    (android.os.SystemClock.elapsedRealtime() - mt009Exec))
                val mt009Shutdown = android.os.SystemClock.elapsedRealtime()
                shutdownIfNeeded()
                android.util.Log.i("MarsTubePerf", "MT-009 tcl_v8_shutdown_ms=" +
                    (android.os.SystemClock.elapsedRealtime() - mt009Shutdown))
                result
            }
        } catch (e: Exception) {""",
     """        val stdout = try {
            val task = java.util.concurrent.FutureTask<String> {
                synchronized(v8Lock) {
                    tclHandler.removeCallbacks(releaseIdleRuntime)
                    val warm = v8Runtime != null
                    val mt009Init = android.os.SystemClock.elapsedRealtime()
                    initRuntime()
                    android.util.Log.i("MarsTubePerf", "MT-009 tcl_v8_init_ms=" +
                        (android.os.SystemClock.elapsedRealtime() - mt009Init))
                    android.util.Log.i("MarsTubePerf", "MT-010A2 v8_session_warm=" + warm)
                    try {
                        val mt009Exec = android.os.SystemClock.elapsedRealtime()
                        val result = runV8(stdin)
                        android.util.Log.i("MarsTubePerf", "MT-009 tcl_v8_exec_ms=" +
                            (android.os.SystemClock.elapsedRealtime() - mt009Exec))
                        result
                    } finally {
                        tclHandler.postDelayed(releaseIdleRuntime, idleLifetimeMs)
                    }
                }
            }
            if (android.os.Looper.myLooper() == tclWorker.looper) {
                task.run()
            } else if (!tclHandler.post(task)) {
                throw IllegalStateException("TCL worker unavailable")
            }
            task.get(20, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: Exception) {"""),
])

patch("youtubeapi/src/main/assets/nsigsolver/tcl.solver.js", [
    ("one compiled function per engine",
     """  // Entry point matching the calling convention used by TclChallengeProvider:""",
     """  // Reuse a single already compiled pure n-function in the same V8 context.
  // A changed player source always forces compilation of the new function.
  // MT-010A2 is a LAB overlay, never enabled in the upstream gitlink.
  let mt010CompiledSource = null;
  let mt010CompiledFunction = null;

  // Entry point matching the calling convention used by TclChallengeProvider:"""),
    ("compile reuse",
     """      const nFunction = new Function(preprocessedPlayer)();""",
     """      let nFunction;
      if (mt010CompiledSource === preprocessedPlayer && mt010CompiledFunction !== null) {
        nFunction = mt010CompiledFunction;
      } else {
        // Drop stale references before parsing the changed player revision.
        mt010CompiledSource = null;
        mt010CompiledFunction = null;
        nFunction = new Function(preprocessedPlayer)();
        mt010CompiledSource = preprocessedPlayer;
        mt010CompiledFunction = nFunction;
      }""")
])
print("MT-010A2: dedicated worker, 45s idle V8 release, one compiled script; LAB only")
