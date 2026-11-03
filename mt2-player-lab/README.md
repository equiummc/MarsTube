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

## MT2-004 Vorbefund: AVD Audio-Clock und physische Eingabe (03.11.2026)

Nutzerbericht bei MT2-003R: Audio kratzt; Ton und Bild subjektiv
1.5-2.0x; Maus-/Tastatur-/Emulator-Fernbedienung direkt nicht bedienbar.
Beweisstand aus `MT2-003R-Playerlog.txt` (ep=1, `fBVriTLty5M`):

- `prepare +471 type=dash-mpd`: **DASH**, nicht SABR; kein SABR-Fehler im Log.
- Erstes Bild `+1331ms`, Media3 Player `READY +1387ms`.
- Videodecoder `c2.goldfish.vp9.decoder`, Audio `c2.android.opus.decoder`.
- Um 00:32:31.472 meldet `MarsTube2TV key=37`
  `Tempo aktuell 1,00x | gewählt 1,00x | Lautstärke 100%`.
- Um 00:32:32.551 bei Seek vorwärts: vorheriger Playhead `18155ms`.
  Zwischen erstem Bild um 00:32:20.713 und diesem Seek liegen 11.84s
  Wanduhr, aber 18.155s Inhalt. **Hinweis auf ca. 1.53-fachen Clock-Drift**,
  kein reproduzierter kontrollierter Ticker und deshalb kein endgültiger
  Messwert. Diese Diskrepanz ist mit einer Android-Emulator-Audio-Clock-
  Fehlfunktion kompatibel, nicht mit der App-Einstellung 1.00x.
- ADB-Tasten wurden vom **Player** verarbeitet: `key=37`,
  `key=22 +10s`, `key=21 -10s`; `position-discontinuity` beweist
  echte Seek-Auslösung. Diese Signale waren **ADB-injiziert** und beweisen
  NICHT die Funktionsfähigkeit einer physischen Host-Tastatur oder der
  grafischen Emulator-Fernbedienung.
- Aus diesem gefilterten NetPath-Log sind **keine AudioTrack-Underruns,
  PCM-Sample-Raten oder Audio-Ausgabezeitstempel** ablesbar. Die genaue
  Ursache des kratzenden Tons ist daher nicht bewiesen.

Bekannte analoge öffentliche Android-Emulator-Fehler:
- androidx/media#5: Wear-AVD spielt Media3 ~2x, obwohl Normaltempo;
  Ursache dort Emulator-Hardwaredecoder; kein Beweis für unseren Android-TV-AVD.
  https://github.com/androidx/media/issues/5
- androidx/media#3210 (2026): AudioTrack-Unterläufe, Glitches und unruhiges
  Pacing bei Media3; auf anderen Geräten, nicht unser Nachweis.
  https://github.com/androidx/media/issues/3210
- Emulator Settings > General > Send keyboard shortcuts to > Virtual device
  prüfen, danach tatsächliche Fensterfokussierung; Android-Doku
  https://developer.android.com/studio/run/emulator-extended-controls

**Nächste Entwicklung:** eigenständige Android-TV-D-pad-Navigation mit
sichtbarem Fokus; kein Phone-UI-Touch als TV-Foundation. Instrumentierte
Audiozeit (AudioTrack timestamp / underruns), monotone Playerposition und
Speed alle 1s im Lab; Emulator-vs-physischer-TV A/B bei gleichem Video.
Kein pauschales Erhöhen/Verringern der Geschwindigkeit, keine Änderungen
an der stabilen MarsTube-1.x.

## MT2-003: Fehler beim Fortsetzen eines zuvor gesehenen Videos

Nutzerbefund 03.11.2026: Die frisch gestartete MT2-002-App zeigte direkt
die SABR-Fehlermeldung bei **12:13 / 53:36** auf dem Video aus dem
vorherigen Test. Bei diesem Start hat das Video **nicht** zwölf Minuten
gespielt. Die Position kann aus der Wiedergabehistorie stammen.
Ein erneuter SABR-Streamabbruch nach 12 Minuten ist aus dem Screenshot
**nicht** ableitbar. Der konkrete Fehler (Transport vs. wiederhergestellter
Playerzustand) bleibt ohne neues ADB-Log offen.

Technischer Befund im gepinnten NewTube-Controller: Der experimentelle,
bevorzugte SABR-Pfad behandelt seine Fehler in onPlayerError **absichtlich
als terminal** (sabrTerminalError), obwohl der VISIONOS-Quellantwort
funktionierende DASH-URLs beiliegen. Das erklärt, weshalb die UI
stoppte statt automatisch weiterzuspielen. Es erklärt allein noch nicht
den Auslöser des Fehlers.

Der optionale MT2-003-Patch \`patch_sabr_recovery.py\` baut genau dort
einen einmaligen, pro Video begrenzten DASH-Rettungsversuch ein:
- Kein Öffnen der Original-App, keine Veränderung der YouTube-Clientwahl.
- Nur wenn dieselbe MediaItemFormatInfo tatsächlich DASH-URLs enthält.
- Sichere Übernahme der Playerposition und playWhenReady-Einstellung.
- Keine wiederholte Rückkehr zu SABR beim selben Titel (kein Wechsel-Loop).
- Unverändert: SABR-only ohne DASH-URLs und andere Fehlerpfade.
- Logs \`mt2-003 sabr-error -> dash-once\` und
  \`mt2-003 sabr-to-dash resumed\` erlauben den Nachweis.

Der Patch hat den **GitHub-Actions-Build und die SABR-Unit-Tests** bestanden:
https://github.com/equiummc/mars-smarttube/actions/runs/41552329669
(Referenz APK ARM64 SHA256 `e35c05c711e8319964ae4fb77dc0212bd557e7eecc4a1f61c2317fbc9e203c23`).
**Die dynamische Fehlerbehandlung im Emulator ist noch nicht abgenommen.**
Die Test-APK wird ggf. anders signiert als MT2-002. Ein Signaturkonflikt bei **identischer Paketkennung** erfordert entweder dieselbe
Signatur oder den Verlust der bisherigen Appdaten; deshalb wird der alte
Referenz-Build **NICHT deinstalliert**. MT2-003R hat eine eigene Paketkennung.
Abnahme: zuvor gespeichertes Video bei ca. 12:13 öffnen, Fehlerfall
eintreten lassen, \`type=dash-mpd\` im selben Video nachweisen, weiter
ab exakt dieser Position spielen; Tasten- und Speed-Tests separat.

### MT2-003R: sichere parallele Test-App (03.11.2026)

Nach INSTALL_FAILED_UPDATE_INCOMPATIBLE fuer
`io.github.aleixrodriala.arc.debug` wurde ausschliesslich in der
isolierten Test-Checkout-Gradle-Flavour-Datei die Anwendungkennung ersetzt:
`de.equiummc.marstube2.lab.debug`. Label: `MarsTube 2.0 Recovery Lab`.
Die alte Referenz-App inklusive der historischen 12:13-Position bleibt auf
dem Android-TV-Emulator bestehen; die neue App hat **eigene leere Daten**.
Testen der Fortsetzung bei 12:13 erfordert deshalb eine frische
Wiedergabehistorie in der neuen App oder manuelles Springen zur Stelle.

- GitHub Actions Build (SABR-Unit-Tests gruen):
  https://github.com/equiummc/mars-smarttube/actions/runs/41554493217
- ARM64 SHA256: `13deadf110c1776b85b2c8bf3cb6ace938dce82f5621c9c8297fe52908e5702a`.
- Verifiziert im kompilierten AndroidManifest: package
  `de.equiummc.marstube2.lab.debug` und neue app-spezifische Authorities.
- Der sichere Installer nutzt **nur emulator-5554**, prueft SHA256,
  entfernt kein Paket, startet bei Bedarf AVD `MarsTube2_TV`.
- Der TV-Remote- und SABR-zu-DASH-Rettungscode ist identisch mit MT2-003.
- Noch offen: Live-Abnahme der Installation, TV-Tasten, echte SABR-Fehlerrettung.

## MT2-002: Isolierte TV-Eingabesteuerung (Quellpatch, nicht MarsTube 2.0 selbst)

Der NewTube-Referenzcode ist **Phone-only**. MT2-002 ergänzt für die Test-APK
mit `mt2-player-lab/patch_tv_input.py` zwei Aktivitäten im **temporären
GitHub-Actions-NewTube-Klon** und kennzeichnet die APK als
`MarsTube 2.0 Input Lab`. Die existierende MarsTube 1.x, ihre Paketkennung und
der originale NewTube-Commit bleiben unangetastet. Die Referenz-App nutzt
weiterhin `io.github.aleixrodriala.arc.debug` und Media3 1.10.1;
die TV-Eingabesteuerung ist ein **Prototyp**, noch keine MarsTube-2.0-Architektur.

| Seite | Eingabe | MT2-002-Funktion |
| --- | --- | --- |
| Startseite | D-pad | Mit sichtbarem blauem Rahmen zwischen Karten navigieren |
| Startseite | OK / Enter | Markierte Karte auswaehlen |
| Player | OK / Enter / Leertaste | Pause / Weiter |
| Player | D-pad links/rechts | -10/+10 Sekunden (nur seekbare Quelle) |
| Player | D-pad hoch/runter | App-interne Lautstaerke +/-10 Prozentpunkte |
| Player | Ziffer 0 | Wieder 1.00x Wiedergabegeschwindigkeit |
| Player | Taste S | 1.00x, 1.25x, 1.50x, 2.00x, 0.75x durchschalten |
| Player | Taste I | Gewaehlte / effektive Geschwindigkeit, App-Lautstaerke anzeigen |
| Player | Menue | Vorhandenes Player-Menue oeffnen |
| Player | Zurueck / Escape | Bestehende NewTube-Backnavigation (unveraendert) |
| Windows | `Save-MarsTubeEmulatorScreenshot.ps1` | PNG in `Bilder\MarsTube2` ablegen |

Bei unbekannter Android-Emulator-Hardware-Tastenzuordnung koennen dieselben
D-pad-Tasten ueber ADB `adb -s emulator-5554 shell input keyevent DPAD_RIGHT`
getestet werden. Der Screenshot wird **vom Emulator** aufgenommen, nicht aus
dem Media3-Decoder extrahiert. DRM-geschuetzte Videoinhalte koennen schwarz
erscheinen. Externes `scrcpy` ist fuer echte Touch-Mauseingaben moeglich;
diese Version enthaelt **noch keine native Maus->Touch-Abbildung**.

Abnahme vor einer freigegebenen APK:
- GitHub-Actions-Compile und SABR-Unit-Tests
- Install in `emulator-5554` parallel zur alten MarsTube 1.x
- Tasten und Fokus an mindestens 5 Karten; Pause/Seek/Lautstaerke/Speed
- Test mit normalem DASH und SABR, je >120s, Fehlerprotokoll
- Keine 1.x-App-IDs, Updater, Tokens oder Daten veraendern

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

## MT2-004: Fire-TV-Cube als primäres Testgerät (Nutzerabnahme 03.11.2026)

Praxisbericht auf echtem Fire TV Cube (2019, AFTR) mit der separat installierten
MT2-004 32-Bit-Media3-Testversion:

- Videowiedergabe und Wechsel zu einem anderen angeklickten Video: sehr flüssig,
  Start praktisch unmittelbar, kein auffälliges Kratzen oder beschleunigtes Tempo.
- Bild-in-Bild/Miniplayer: kleines laufendes Video unten, während oben
  weitere Videos durchsucht und gestartet werden können; positiv beobachtet.
- Fernbedienung: grundsätzliche Bedienung funktioniert, aber holprig;
  unteres Navigationsmenü per Fernbedienung nicht erreichbar.
- Shorts: im getesteten Bereich nicht angezeigt. Noch offen, ob Feed/Navigation,
  Datenlage oder eigener Defekt; keine Diagnose ohne konkreten Feed/Log.
- PC-/Android-TV-Emulator zuvor: kratzender Ton, scheinbar ca. 1.5x-2x
  Wiedergabe trotz Media3 speed 1.0x, fehlerhafte direkte Host-Tasten.
  Unterschied deutet auf Emulator-/Host-Konfiguration als Ursache hin,
  beweist aber nicht, dass alle PCs betroffen wären.

**Verbindliche weitere Teststrategie:** Fire TV Cube (AFTR) ist ab sofort
Haupttestgerät für Wiedergabe, Audio, Navigation, Startzeiten und reale
Fernbedienung. Thomson-TV bleibt sekundäres Kompatibilitäts-/Performanceziel.
Android-Emulator nur noch ergänzend für schnelle Regressionen, kontrollierte
Fehlerfälle und Instrumentierung; keine finale Qualitätsbewertung anhand
von Emulator-Audio oder Emulator-Wiedergabetempo.

**Priorisierte MT2-005 UI-Arbeit:** native TV-Fokusnavigation für untenliegendes
Menü, Richtung/Enter/Back, klar erkennbarer Fokus und kontrollierte
Miniplayer-Fokusübergabe. Shorts anschließend gezielt mit eigener Navigation
und Wiedergabe testen. Keine Veränderung der stabilen MarsTube-1.x-App.
