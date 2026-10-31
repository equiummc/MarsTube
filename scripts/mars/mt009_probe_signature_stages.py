#!/usr/bin/env python3
"""MT-009: diagnostic-only probes around YouTube signature/n decoding.

Run AFTER mt008_probe_media_core.py on pinned MediaServiceCore
40fec843eb24bc264e9c8b429fcee25068e230e2. Fail closed if source shifts.
No changes to signature input/output, PoTokens, auth or V8 lifecycle.
Run in the isolated MT-009 build worktree only; never commit the altered gitlink.
"""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / "MediaServiceCore"
PINNED = "40fec843eb24bc264e9c8b429fcee25068e230e2"
CHECK_ONLY = "--check" in sys.argv[1:]
if subprocess.check_output(["git", "-C", str(CORE), "rev-parse", "HEAD"], text=True).strip() != PINNED:
    raise SystemExit("MT-009 STOP: unexpected MediaServiceCore revision")

def patch(path, replacements):
    f = CORE / path
    source = f.read_text(encoding="utf-8")
    result = source
    for label, old, new in replacements:
        if result.count(old) != 1:
            raise SystemExit(f"MT-009 STOP: {path} anchor {label} expected 1, got {result.count(old)}")
        result = result.replace(old, new)
    if not CHECK_ONLY:
        f.write_text(result, encoding="utf-8", newline="\n")
    print(f"MT-009 {('checked' if CHECK_ONLY else 'instrumented')}: {path}")

patch("youtubeapi/src/main/java/com/liskovsoft/youtubeapi/videoinfo/VideoInfoServiceBase.java", [
    ("bulk signature phase",
     """        Pair<List<String>, List<String>> result = mAppService.bulkSigExtract(videoInfo.getClient(), extractNParams(urlHolders), extractSParams(urlHolders));""",
     """        long mt009BulkStart = android.os.SystemClock.elapsedRealtime();
        Pair<List<String>, List<String>> result;
        try {
            result = mAppService.bulkSigExtract(videoInfo.getClient(), extractNParams(urlHolders), extractSParams(urlHolders));
        } finally {
            android.util.Log.i("MarsTubePerf", "MT-009 decipher_bulk_ms=" +
                    (android.os.SystemClock.elapsedRealtime() - mt009BulkStart) +
                    " format_entries=" + urlHolders.size());
        }"""),
    ("apply signature and n",
     """        String poToken = PoTokenGate.getPoToken(videoInfo.getClient(), videoInfo.getVideoDetails().getVideoId());
        videoInfo.setPoToken(poToken);
        applySessionPoToken(urlHolders, poToken);""",
     """        long mt009ApplyStart = android.os.SystemClock.elapsedRealtime();
        String poToken = PoTokenGate.getPoToken(videoInfo.getClient(), videoInfo.getVideoDetails().getVideoId());
        videoInfo.setPoToken(poToken);
        applySessionPoToken(urlHolders, poToken);
        android.util.Log.i("MarsTubePerf", "MT-009 decipher_token_apply_ms=" +
                (android.os.SystemClock.elapsedRealtime() - mt009ApplyStart));"""),
])

patch("youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/AppServiceCoreCached.java", [
    ("extractor path",
     """    private PlayerDataExtractor getPlayerDataExtractorSync(@Nullable AppClient client, String playerUrl) {
        synchronized (mPlayerSync) {
            return firstValidExtractor(
                    client,
                    playerUrl,
                    check(getData().getAppInfo()) ? getData().getAppInfo().getPlayerUrl() : null,
                    AppConstants.playerUrls.get(0)
            );
        }
    }""",
     """    private PlayerDataExtractor getPlayerDataExtractorSync(@Nullable AppClient client, String playerUrl) {
        long mt009Start = android.os.SystemClock.elapsedRealtime();
        synchronized (mPlayerSync) {
            try {
                return firstValidExtractor(
                        client,
                        playerUrl,
                        check(getData().getAppInfo()) ? getData().getAppInfo().getPlayerUrl() : null,
                        AppConstants.playerUrls.get(0)
                );
            } finally {
                android.util.Log.i("MarsTubePerf", "MT-009 extractor_lookup_ms=" +
                        (android.os.SystemClock.elapsedRealtime() - mt009Start));
            }
        }
    }"""),
])

patch("youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/playerdata/TclPlayerDataExtractor.kt", [
    ("TCL player fetch/init vs solve",
     """        val nResults = validNParams?.let { params -> playerCode?.let { TclChallengeProvider.solveN(fixedPlayerUrl, it, params) } }
        val nProcessed = nResults?.let { results -> nParams?.map { results[it] } }""",
     """        val mt009PlayerStart = android.os.SystemClock.elapsedRealtime()
        val resolvedPlayerCode = if (validNParams != null) playerCode else null
        android.util.Log.i("MarsTubePerf", "MT-009 tcl_player_code_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009PlayerStart) +
            " required=" + (validNParams != null))
        val mt009SolveStart = android.os.SystemClock.elapsedRealtime()
        val nResults = validNParams?.let { params -> resolvedPlayerCode?.let { TclChallengeProvider.solveN(fixedPlayerUrl, it, params) } }
        android.util.Log.i("MarsTubePerf", "MT-009 tcl_solve_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009SolveStart) +
            " executed=" + (validNParams != null && resolvedPlayerCode != null))
        val nProcessed = nResults?.let { results -> nParams?.map { results[it] } }"""),
])

patch("youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/playerdata/WebPlayerDataExtractor.kt", [
    ("web solver call",
     """        val result = V8ChallengeProvider.bulkSolve(listOfNotNull(nRequest, sRequest))""",
     """        val mt009Start = android.os.SystemClock.elapsedRealtime()
        val result = V8ChallengeProvider.bulkSolve(listOfNotNull(nRequest, sRequest))
        android.util.Log.i("MarsTubePerf", "MT-009 web_bulk_solve_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009Start) +
            " challenge_types=" + listOfNotNull(nRequest, sRequest).size)"""),
])

patch("youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/nsigsolver/impl/TclChallengeProvider.kt", [
    ("TCL preprocessed cache lookup",
     """        val cachedPreprocessed = CacheService.load(cacheSection, cacheKey)?.code""",
     """        val mt009CacheStart = android.os.SystemClock.elapsedRealtime()
        val cachedPreprocessed = CacheService.load(cacheSection, cacheKey)?.code
        android.util.Log.i("MarsTubePerf", "MT-009 tcl_cache_lookup_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009CacheStart) +
            " hit=" + (cachedPreprocessed != null))"""),
    ("TCL V8 lifecycle",
     """            synchronized(v8Lock) {
                initRuntime()
                val result = runV8(stdin)
                shutdownIfNeeded()
                result
            }""",
     """            synchronized(v8Lock) {
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
            }"""),
    ("TCL V8 startup script and native creation",
     """        v8Runtime = V8.createV8Runtime()
        runV8(constructCommonStdin())""",
     """        val mt009CreateStart = android.os.SystemClock.elapsedRealtime()
        v8Runtime = V8.createV8Runtime()
        android.util.Log.i("MarsTubePerf", "MT-009 tcl_v8_native_create_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009CreateStart))
        val mt009ScriptStart = android.os.SystemClock.elapsedRealtime()
        val mt009Script = constructCommonStdin()
        android.util.Log.i("MarsTubePerf", "MT-009 tcl_v8_load_script_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009ScriptStart))
        val mt009BootStart = android.os.SystemClock.elapsedRealtime()
        runV8(mt009Script)
        android.util.Log.i("MarsTubePerf", "MT-009 tcl_v8_boot_script_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009BootStart))"""),
    ("TCL solution decode",
     """        val output: SolverOutput = try {
            Gson().fromJson(stdout, solverOutputType)""",
     """        val mt009JsonStart = android.os.SystemClock.elapsedRealtime()
        val output: SolverOutput = try {
            Gson().fromJson(stdout, solverOutputType)"""),
    ("TCL JSON timing",
     """        if (output.type == "error") {""",
     """        android.util.Log.i("MarsTubePerf", "MT-009 tcl_json_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009JsonStart))
        if (output.type == "error") {"""),
])

patch("youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/nsigsolver/impl/V8ChallengeProvider.kt", [
    ("generic V8 lifecycle",
     """        synchronized(v8Lock) {
            initRuntime()

            val result = runV8(stdin)

            shutdownIfNeeded()

            return result
        }""",
     """        synchronized(v8Lock) {
            val mt009Init = android.os.SystemClock.elapsedRealtime()
            initRuntime()
            android.util.Log.i("MarsTubePerf", "MT-009 web_v8_init_ms=" +
                (android.os.SystemClock.elapsedRealtime() - mt009Init))

            val mt009Exec = android.os.SystemClock.elapsedRealtime()
            val result = runV8(stdin)
            android.util.Log.i("MarsTubePerf", "MT-009 web_v8_exec_ms=" +
                (android.os.SystemClock.elapsedRealtime() - mt009Exec))

            val mt009Shutdown = android.os.SystemClock.elapsedRealtime()
            shutdownIfNeeded()
            android.util.Log.i("MarsTubePerf", "MT-009 web_v8_shutdown_ms=" +
                (android.os.SystemClock.elapsedRealtime() - mt009Shutdown))
            return result
        }"""),
    ("web V8 startup script and native creation",
     """        v8Runtime = V8.createV8Runtime()
        runV8(constructCommonStdin()) // ignore the result, just warm up""",
     """        val mt009CreateStart = android.os.SystemClock.elapsedRealtime()
        v8Runtime = V8.createV8Runtime()
        android.util.Log.i("MarsTubePerf", "MT-009 web_v8_native_create_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009CreateStart))
        val mt009ScriptStart = android.os.SystemClock.elapsedRealtime()
        val mt009Script = constructCommonStdin()
        android.util.Log.i("MarsTubePerf", "MT-009 web_v8_load_script_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009ScriptStart))
        val mt009BootStart = android.os.SystemClock.elapsedRealtime()
        runV8(mt009Script) // ignore the result, just warm up
        android.util.Log.i("MarsTubePerf", "MT-009 web_v8_boot_script_ms=" +
            (android.os.SystemClock.elapsedRealtime() - mt009BootStart))"""),
])

print("MT-009 instrument complete: durations and format counts only, no URLs/IDs/tokens")
