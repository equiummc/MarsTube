# MarsTube 2.0 – MT2-000: Media3 + YouTube SABR, isolierter Machbarkeitstest

Stand: 02.11.2026. **Kein Umbau von MarsTube 1.x.** Experiment-Branch:
`experiment/mt2-media3-sabr-proof-20261102` (Ausgang: `mars/stable`).

## Ziel und genaue Abgrenzung

Bevor wir die neue MarsTube-2.0-Videostruktur erstellen, prüfen wir den vollständigen
Pfad **YouTube-Metadaten → SABR-Adapter → Media3 MediaSource → Hardware-Decoder → sichtbares Video**
auf Thomson Android 14 und Fire TV. Ein erfolgreicher Gradle-Build ist noch kein echter
SABR-Nachweis. Die NewTube-Referenz-App ist eine **separate technische Probe**, **noch
keine Integration in MarsTube 2.0**.

Nach dieser Probe wird der NewTube-Modulcode (mit Quellen-/Lizenzhinweisen) in das
eigenständige MarsTube-2.0-Projekt überführt und mit unserer YouTube-Metadatenschicht
verbunden. Die existierende MarsTube-1.x-Paketkennung, Daten und Player werden nicht
ersetzt. Vor einer Freigabe stehen Geräte- und Dauerlaufprüfungen.

## Nachprüfbare Quellen / gewählter eingefrorener Stand

- NewTube: https://github.com/aleixrodriala/newtube
- Pinned Referenz-Commit: `6f96a1b42bb9272935360d7e5aec34d35f2753f0`
- SABR-Media3-Modul: `sabr-media3/`
- Player-Anbindung: `smarttubetv/src/stmobile/java/com/newtube/mobile/player/Media3PlayerController.java`
- Metadaten-Adapter: `smarttubetv/src/stmobile/java/com/newtube/mobile/player/SabrFormatAdapter.java`
- Dokumentation: `docs/mobile-port/SABR-MEDIA3-2026-09-08.md`, `HANDOFF.md`
- NewTube implementiert SABR mit Media3 **1.10.1**. Diese API-Fassung wird für
  den ersten Machbarkeitsnachweis **nicht** gleichzeitig aktualisiert.
- AndroidX Media3 **1.11.1** ist die vorgesehene stabile Zielversion.
  Upgrade / Inkompatibilitäten (insbesondere `SabrSubtitleSourceFactory`) erhalten einen
  **separaten** Durchlauf nach dem 1.10.1-Basistest.

## Der kritische SABR-Haken

SABR ist in Media3 **nicht** eingebaut. Nur `media3-exoplayer` zu deklarieren
reicht nicht. Für echten YouTube-SABR sind Metadaten, echte Formatidentitäten,
`serverAbrStreamingUrl`, `videoPlaybackUstreamerConfig`, Protobuf/UMP, Abrufsteuerung,
Segment-Demultiplexing und eine `MediaSource` erforderlich.

NewTube dokumentiert funktionierende SABR-Wiedergabe mit bestimmten Clients.
Andererseits fällt der SABR-ONLY-Fallback ohne erforderliche Attestation/PO-Token
bei bestimmten Clients nach ca. **60 Sekunden** aus. **SABR niemals pauschal als
Allheilmittel für HTTP 403 aktivieren.** Der erste Versuch verwendet den originalen
NewTube-Referenzplayer; DASH bleibt Standard, SABR wird nur für den Test gewählt.

## Windows: Referenzbuild / ADB / Diagnose

Auf einem Windows-PC mit Git, JDK 17, Android SDK (nach NewTube-
Toolchain gegebenenfalls API 37) und installiertem Android SDK Platform Tools:

```powershell
# Im Experiment-Branch des MarsTube-Repos:
.\mt2-player-lab\Invoke-Media3SabrLab.ps1 -Mode Build

# Standard: Thomson; kann mit -Device <adb-serial> angepasst werden.
.\mt2-player-lab\Invoke-Media3SabrLab.ps1 -Mode Install -Device 192.168.41.53:5555

# Nach dem realen Playbacktest:
.\mt2-player-lab\Invoke-Media3SabrLab.ps1 -Mode Logs -Device 192.168.41.53:5555
```

Das Skript klont NewTube nach `E:\Mars-TV\Projekte\marstube2\player-lab\newtube-reference`,
prüft den exakt festgehaltenen Git-Commit, initialisiert NewTube-Submodule,
passt ausschließlich dort die Linux-Protoc-Dependency für Windows an,
führt `:sabr-media3:testDebugUnitTest` und
`:smarttubetv:assembleStmobileDebug` aus. Es installiert ausschließlich die
**separate** Referenz-App (erwartete Paketkennung
`io.github.aleixrodriala.arc.debug`). Die NewTube-Oberfläche ist für
Telefone ausgelegt und auf TV-Fernbedienungen möglicherweise eingeschränkt;
gegebenenfalls per ADB starten oder mit angeschlossener Maus bedienen.

Die Logfunktion speichert ausgewählte Wiedergabe-Fehler lokal; kein Upload.
Die rohe Quelle oder Authentifizierungsdaten nicht unbesehen weitergeben.

## Testmatrix auf Thomson und Fire TV

| Prüfschritt | Nachweis |
| --- | --- |
| Quellcode kompiliert | `sabr-media3`-Unit-Tests und Debug-APK erfolgreich |
| Echte Quelle | Im Log ist **SABR** statt nur DASH für das Video nachgewiesen |
| Anfang | Erstes sichtbares Bild, inklusive Bild/Ton-Synchronität |
| Dauerlauf | Mindestens **120 Sekunden** ohne Stopp um die 60-s-Marke |
| Spulen | Je drei Sprünge zurück und vor, keine schwarzen Dauerbilder |
| Wiederholen | 5 direkt aufeinanderfolgende Videostarts |
| Hardware/Qualität | Tatsächlicher Decoder, 1080p60; höhere Auflösungen nur, wenn Gerät sie unterstützt |
| Fehler | HTTP 403, `ATTESTATION_REQUIRED`, Rebuffer, EOF, `onPlayerError` auswerten |
| Geräte | Thomson Android 14; Fire TV Cube separat |

**Erfolgskriterium**: SABR-Nutzung nachweisbar, 120-s-Dauerlauf und alle Seeks
auf den vorgesehenen Geräten erfolgreich. Anschließend erst Medien-Adapter
mit den MarsTube-Metadaten verbinden, mit der stabilen 1.x bei identischem
Videomaterial messen und Media3 1.11.1 gezielt migrieren.

**Fehlschlag**: Bei 403 / Attestation / Abbruch nach 60 s protokollieren,
keine Produktivintegration, 1.x unverändert. Falls die Referenz-App nicht
bedienbar ist, erfordert der nächste Schritt einen dedizierten Android-TV-
Test-Activity-Wrapper oder einen instrumentierten Geräte-Test.

## Projektstatus

- Isolierter Git-Branch angelegt.
- Reproduzierbares Windows-Skript hinterlegt.
- GitHub-Actions-Build/Unit-Test als zusätzlicher, geräteunabhängiger Check.
- **Nicht nachgewiesen:** APK-Build erfolgreich, Installation auf TV, realer
  SABR-Stream >120 s oder Übernahme in eigene MarsTube-Videostruktur.
