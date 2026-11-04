# MarsTube – dauerhaftes Projektwissen für Codex und OpenCode

**Stand:** 04.11.2026 · **Produkt:** MarsTube 2.0 · **Repository:** https://github.com/equiummc/MarsTube

Dieses Dokument ist die **dauerhafte technische Übergabe**. Vorhandene Informationen sind nach *Ist-Zustand*, *Ziel* und *offene Verifikation* unterschieden. Es ersetzt weder Tests noch den freigegebenen 0.0.10-Arbeitsauftrag.

## 1. Produktversprechen

MarsTube soll eine **schlanke, schnelle, sichere und optisch hochwertige** Medien-App sein, die eine **hohe tatsächliche Videoqualität** liefert: keine künstliche Obergrenze bei 1080p, 10 Mbit/s oder 30 FPS; 4K/UHD, hohe FPS, HDR, AV1/VP9 bei geeigneter Quelle und kompatibler Hardware; intelligente automatische Auswahl und optionale manuelle Qualitätsgrenzen. Schwache Hardware muss ohne unnötige Oberflächeneffekte flüssig bleiben. Details: [PRODUKTZIEL.md](PRODUKTZIEL.md).

Freigegebenes visuelles Ziel: **neonblauer MarsTube-TV-Player** (dunkelblauer Hintergrund, M-Logo, transparente blau getönte Bedienleiste, Fortschritt **oberhalb** der Kontrollelemente, elegante weiße/cyanfarbene Vektorsymbole, gerundete Fokusrahmen, untere Videovorschau). **Das originale Freigabebild ist noch nicht nachweislich eingecheckt**. Nicht eigenmächtig Ersatzdesigns erfinden oder als die Vorlage deklarieren.

## 2. Versionen und Branchkonzept

- Vorherige Testbasis: 0.0.9; Entwicklungsbranch `feature/mt2-009-shorts-blue-ui-20261104`.
- Aktueller Entwicklungsauftrag: **0.0.10**, Branch `feature/mt2-010-player-settings-ui-20261104`.
- Repository-Default `master` enthält noch eine ältere SmartTube-Basis und dessen historische README. **Nicht daraus ableiten**, dass 0.0.10 hier direkt als fertige normale Gradle-App liegt.
- **Zukünftige Produktkanäle:** MarsTube **Stable** und **Alpha**, die auf gemeinsamer Codebasis beruhen, aber **getrennte Android-Paketkennungen und App-Daten** besitzen sollen. Kanäle sind derzeit *geplant*, nicht fertig. Stable soll Media3 **1.11.1**, Alpha zunächst **1.12.0-alpha02** separat evaluieren. Ein Media3-Player pro APK.
- **Referenz-Player:** Gepinnter externer NewTube-Commit `6f96a1b42bb9272935360d7e5aec34d35f2753f0`, derzeit Media3 **1.10.1**. Upgrade-Simulationen sind **noch keine verifizierten Integrationen**.
- Sicherer Wechsel auf eine wirklich eigenständige MarsTube-Quell-/Buildstruktur ist Ziel. Erforderliche OSS-Lizenzen und Notices dürfen nicht entfernt werden. Siehe [MARSTUBE-EIGENSTAENDIGKEIT.md](MARSTUBE-EIGENSTAENDIGKEIT.md).

## 3. Buildarchitektur (Ist-Zustand im GitHub-Branch)

- Der Workflow `/.github/workflows/mt2-media3-sabr-lab.yml` lädt den gepinnten Referenzcode, initialisiert dessen Submodule, führt die Python-Overlays aus und baut/testet Android.
- Versionierte eigene Quellen, Patches und Skripte: `mt2-player-lab/`. Auf dem Windows-PC wurde ein **lokaler** Klon unter `E:\MarsTube` mit einem nicht versionierten Referenzcheckout unter `.mt2-build/newtube-reference` verwendet.
- Relevante UI-Klassen: `mt2-player-lab/MarsTvShell.java` (TV-Player/Steuerung), `MarsTvRail.java` (linkes Menü); neue 0.0.10-Dateien `MarsTvPlayerPrefs.java`, `MarsTvSettingsPages.java`, `MarsTvPlayerPrefsTest.java`, `assets/drawable/` und `patch_010_player_settings_ui.py` (auf GitHub vorhandenen Status vor der nächsten Arbeit erneut prüfen).
- In der Overlay-Reihenfolge sind besonders `patch_008_tv_player.py`, `patch_009_tv_shorts_ui.py`, `patch_010_player_settings_ui.py` relevant. Patch-Anwendung ist **Teil des Builds**; eine lokal editierte Datei im externen Checkout genügt nicht.
- **Lokaler Nachweis aus dem Nutzerprotokoll:** `build-010-local.log` meldete am 04.11.2026 `BUILD SUCCESSFUL in 57s`, 241 Gradle-Aufgaben inkl. `testStmobileDebugUnitTest` und `assembleStmobileDebug`. Dies belegt einen erfolgreichen lokalen Build und Ausführung der Test-Task, **nicht** bestandene reale Geräte-/SABR-Tests. Der letzte OpenCode-Stand wird in [PROJEKTSTATUS.md](PROJEKTSTATUS.md) nachgeführt.
- Installationen dürfen nicht unbeabsichtigt vorhandene Apps ersetzen. APK-Datei, Signatur, Paket-ID und Gerätekompatibilität einzeln belegen.

## 4. Player, SABR und Media3 (Ist-Zustand, Risiken)

- **Media3** verantwortet MediaSource/Trackauswahl/Player-Engine und Hardwaredecoder; **SABR** ist ein separates Streamingprotokoll und eine zusätzliche `MediaSource`-Implementierung, kein integriertes Media3-Feature.
- In der Referenz: `sabr-media3/src/main/java/com/newtube/sabr/SabrMediaSource.java` (Loader, Segmentpuffer, Trackauswahl, Seek, UMP-Transport); `SabrProtocol.java` (zustandsabhängiges UMP-Protokoll); `SabrStreamInfo.java` (validierte Streammetadaten), `smarttubetv/src/stmobile/java/com/newtube/mobile/player/Media3PlayerController.java` (Playerwechsel/Fallback), `SabrFormatAdapter.java` (Metadaten-Adapter).
- **Verifizierter technischer Risikopunkt:** `SabrMediaSource.TrackStream.onLoadError` liefert bisher `Loader.DONT_RETRY_FATAL`; nicht jede kurzzeitige Störung wird auf dieser Schicht automatisch wiederholt. Ein Retry-Konzept muss zwischen vorübergehendem Netzausfall und permanenter Protokoll-/Zugriffsablehnung unterscheiden und Backoff plus Obergrenzen nutzen.
- **API-Kopplung:** `sabr-media3/src/main/java/androidx/media3/exoplayer/source/SabrSubtitleSourceFactory.java` verwendet eine versionsspezifische package-private-Funktionalität von Media3 1.10.1; bei Upgrade auf 1.11.1/1.12-alpha02 **zwingend neu kompilieren und Untertitel auf Geräten testen**.
- SABR-Störungen können von sich ändernden Serverantworten, Metadaten, Stream-Zugriffsbedingungen, Netzwerk, Gerätedecodern, Segment/Seek-Zustand oder inkompatiblen Media3-APIs stammen. Diagnose soll den tatsächlich verwendeten Streamingpfad nennen; keine Erfolgsmeldung nur anhand der Einstellung. Wo passende, zulässige DASH-/HLS-Alternativen vorhanden sind, kontrollierter Fallback statt schwarzer Bildschirm; keine unendlichen Retries oder künstliche Umgehung von Zugriffsschutz.
- 4K/HDR hängt von Quelle, Decoder, Bildschirm, Protokoll und tatsächlich angebotenen Tracks ab. Ein Media3-Upgrade garantiert nicht automatisch höhere Auflösung.

## 5. Verbindliche UI- und Bedienanforderungen

- TV-Steuerung vollständig per D-Pad: OK kurz Play/Pause, OK gehalten temporär 2×, bei Loslassen wieder 1×; BACK im Player zurück zur linken Navigation **bei fortgesetzter Wiedergabe**, BACK in Untermenüs schließt zuerst Dialog, keine Fokusfallen.
- Shorts: native **9:16**-Darstellung zentriert ohne Streckung, möglichst drei komplett sichtbare 9:16-Vorschaukarten nebeneinander; bei wenig Platz adaptive Anzahl; scharfe Vorschau, sauberes erstes Bild, kurzer und störungsfreier Wechsel. Oben/Unten/Next/Previous der Fernbedienung sinnvoll behandeln.
- Player: Fortschrittsleiste über Controls, Like/Dislike links, hochwertige statt unscharfer Icons, aktuelle Auflösung/Codec/Bitrate/Protokoll nur im **Info-Dialog**, nicht dauerhaft im Bild. Unterer horizontaler Vorschau-Streifen.
- Einstellungsstruktur: Video & Wiedergabe (Qualität, FPS, Codec, HDR, Bitrate Auto oder optional 3/5/10/15/25 Mbit/s, Puffer, Startmodus), Streaming/SABR, Darstellung, Bedienung, Untertitel, Konto/Info. Echte wirksame Einstellungen, keine Dopplungen und Fokusfallen.
- TV: Downloads ausblenden; nur sinnvolle, aktivierte Miniplayer-Funktion zeigen. Smartphone später mit angepasster Touch-/Hoch-/Querformat-Oberfläche bei gemeinsamem Kern.
- **Zielhardware:** Thomson Android 14 (~1,45 GB RAM), Fire TV Cube (~2 GB), Sony BRAVIA Android 10; alle drei auf ihre Fähigkeiten testen.

## 6. Sicherheit, Lizenzen und Performance

- Nur benötigte Berechtigungen, eingeschränkte Android-Exports, gültige TLS-Prüfung, keine sensiblen Daten in Logs, Schutz von Tokens, sicherer Update-/Signaturprozess, Dependency- und Secret-Scans sowie Fehlerbehandlung mit Ressourcenlimits.
- Externe Open-Source-Bestandteile dürfen mit eigenen Funktionen kombiniert werden, **aber Lizenz- und Copyrightnotices bleiben erhalten**. Sichtbar ausschließlich „MarsTube“, technische Herkunftsnachweise bleiben.
- Auf schwachen TVs keine dauerhaft aufwendigen Blur-/Partikeleffekte oder mehrere gleichzeitig laufende Decoder nur zur Dekoration. Messwerte für First Frame, RAM, Fehler und Puffer dokumentieren.

## 7. Arbeitsweise und Fortschritt

Vorhandene Git-Änderungen von OpenCode **nicht überschreiben**. Erst Branch + Status prüfen, dann nur relevante Dateien bearbeiten. Gewöhnliche PowerShell-Anweisungen **immer mit ausdrücklicher Terminalangabe** geben.

**Abschlussbericht jeder Entwicklungsaufgabe**: [PROJEKTSTATUS.md](PROJEKTSTATUS.md) ergänzen mit SHA, Änderungen, durchgeführten Tests und Ergebnis, APK-Pfad/Signatur falls verifiziert, noch offenen Punkten und nächstem Schritt. Ein erfolgreicher Compile/Unit-Test ist nicht gleich „auf Fire TV und Thomson geprüft“.

## 8. Quellen / maßgebliche Dokumente

- [AGENTS.md](../AGENTS.md) – verpflichtende Regeln für Entwicklungsagenten
- [PROJEKTSTATUS.md](PROJEKTSTATUS.md) – belegter Status, To-dos, Übergabe
- [PRODUKTZIEL.md](PRODUKTZIEL.md) – Produktvision und Sicherheit
- [MARSTUBE-EIGENSTAENDIGKEIT.md](MARSTUBE-EIGENSTAENDIGKEIT.md) – Stable/Alpha, Migration, Media3
- [MT2-010-ARBEITSAUFTRAG.md](../mt2-player-lab/MT2-010-ARBEITSAUFTRAG.md) – ausführlicher Arbeitsumfang
- [.github/workflows/mt2-media3-sabr-lab.yml](../.github/workflows/mt2-media3-sabr-lab.yml) – tatsächlicher GitHub-CI-Build
