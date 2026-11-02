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

Auf einem Windows-PC mit Git, JDK 17, Android SDK 37.0 (Paketname `platforms;android-37.0`) und installiertem Android SDK Platform Tools:

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
installiert nötigenfalls die Android-SDK-Plattform 37.0 und prüft deren JAR,
führt `:sabr-media3:testDebugUnitTest` und
`:smarttubetv:assembleStmobileDebug` aus. Es installiert ausschließlich die
**separate** Referenz-App (erwartete Paketkennung
`io.github.aleixrodriala.arc.debug`). Die NewTube-Oberfläche ist für
Telefone ausgelegt und auf TV-Fernbedienungen möglicherweise eingeschränkt;
gegebenenfalls per ADB starten oder mit angeschlossener Maus bedienen.

Die Logfunktion speichert ausgewählte Wiedergabe-Fehler lokal; kein Upload.
Die rohe Quelle oder Authentifizierungsdaten nicht unbesehen weitergeben.

## Emulator-Versuch auf Windows (02.11.2026)

- GitHub Actions Build der gepinnten NewTube-Referenz erfolgreich:
  https://github.com/equiummc/mars-smarttube/actions/runs/41529437077
- Windows-Android-TV-Emulator `MarsTube2_TV` / `emulator-5554` eingerichtet.
- ARM64-Debug-App `io.github.aleixrodriala.arc.debug` erfolgreich installiert.
- Startseite und normale YouTube-Wiedergabe im Emulator durch Nutzertest bestätigt.
- **Noch kein SABR-Nachweis**: Ein funktionierendes Video kann DASH gewesen sein.
- Native Emulator-Maus verarbeitet die Touch-UI nicht, ADB-**touchscreen tap** dagegen schon.
  Für die PC-Tests `scrcpy --serial emulator-5554 --mouse=sdk --no-audio` verwenden.

### SABR-Diagnose und kontrollierter DASH-Gegenversuch

Das Skript `Test-Media3SabrEmulator.ps1` schreibt nur Einstellungen auf
**emulator-Serials**, stoppt nur die separate Debug-App und filtert Wiedergabe-Logs.
Die experimentelle SABR-Option ist im Original ausgeschaltet. Der Test
setzt nur `debug.arc.sabr_vod=1` (SABR-Versuch) oder `=0` (DASH-Vergleich).
Die Clientwahl bleibt bei NewTubes normaler Logik; der problematische
SABR-only-Fallback ist deaktiviert. Keine Kontotoken, PO-Token oder
Schutzmechanismen umgehen. Falls normale Metadaten keinen SABR-Pfad anbieten,
ist das Ergebnis korrekt *DASH* und noch kein SABR-Nachweis.

```powershell
.\mt2-player-lab\Test-Media3SabrEmulator.ps1 -Mode Sabr
# Ein normales Video mindestens 120 s ansehen, vor/zurueck spulen
.\mt2-player-lab\Test-Media3SabrEmulator.ps1 -Mode Report
# Fuer Vergleich: -Mode Dash; nach neuem Video -Mode Report
# Maus via scrcpy, sofern installiert: -Mode Mouse
```

Nur `NetPath prepare type=sabr-vod` beweist die SABR-Quelle; ein Bild oder
eine aktivierte Option allein **nicht**. Der Report speichert eine gefilterte
Ausgabe unter `Dokumente\MarsTube2\Logs` und laedt nichts hoch.

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
- **Nachgewiesen:** Referenz-APK-Build erfolgreich, ARM64-App im Windows-Emulator,
  Media3-DASH-Wiedergabe und **echte SABR-Quellenauswahl** (`prepare type=sabr-vod`).
- SABR-1 (`fBVriTLty5M`): erstes Bild +1214 ms, nach +143529 ms terminaler
  `SabrException(io_or_container_HttpDataSourceException)`; Ursache (Netzwerk,
  Emulatorlast oder SABR-Implementierung) noch offen.
- DASH-Vergleich (dieselbe ID): erstes Bild +2412 ms, knapp 4 Minuten
  beobachteter Protokollzeitraum ohne Playerfehler.
- SABR-Wiederholung: erstes Bild +2092 ms um 23:22:23, zweites Video
  `EnTBM2xk9yA` mit neuer `type=sabr-vod`-Quelle um 23:28:30 und erstem Bild
  +730 ms. **Keine `load[E]`- oder `error +`-Zeilen im gefilterten Testlog.**
  Die 6:08 Minuten zwischen den Videostarts sind keine lückenlose
  Playhead-/Buffer-Telemetrie und beweisen nicht allein eine ununterbrochene
  6-Minuten-Wiedergabe. Eine momentane PC-Lastspitze beim ersten Fehler ist
  möglich, aber nicht belegt.
- **Noch ausstehend:** kontrollierter Seek-Test mit SABR, vollständiger
  Dauerlauf/Qualitäts- und Decoder-Nachweis, reale Thomson-/Fire-TV-Tests,
  Übernahme in eine eigene MarsTube-2.0-Videostruktur.
