# MarsTube – Testreihe für den internen Updater (Stand 07.11.2026)

Ziel: Die wiederholte Falschmeldung **»Version 0.0.20 ist aktuell«** beim vorhandenen Release **0.0.21** ursächlich eingrenzen. Keine neue App-Version oder produktive Release-Änderung, bis Ursache und reproduzierbarer Fix belegt sind.

Vergleich: der von MarsTube gepinnte [NewTube-Stand v1.17.1](https://github.com/aleixrodriala/newtube/tree/6f96a1b42bb9272935360d7e5aec34d35f2753f0) samt `SharedModules/appupdatechecker2`; MarsTube-Kanal `de.equiummc.marstube2` und Github-Releases. Keine absichtliche Entfernung von Android-Paket-, Signatur- oder Berechtigungsprüfungen.

## Automatische Prüfungen auf GitHub

Test-Workflow: [MarsTube Update E2E Testreihe](https://github.com/equiummc/MarsTube/actions/workflows/mt2-update-investigation-ci.yml). Er führt drei voneinander unabhängige serverseitige Testblöcke aus und veröffentlicht **keine APK**.

| Test | Vorgehen | Erfolgskriterium |
|---|---|---|
| T01: MarsTube Live-Feed | `releases/latest/download/marstube-tv.json` direkt abrufen; echte HTTP-Weiterleitungen verfolgen | HTTP 200, JSON lesbar, Version `0.0.21` mit Code `12021` |
| T02: MarsTube Cache | Dieselbe URL mit einmaliger `mars_check`-Query aus der installierten 0.0.20 aufrufen | dieselbe aktuelle Version wie ohne Parameter |
| T03: NewTube Referenz | Ersten und zweiten offiziellen NewTube-Feed sowie festes v1.17.1-Release abfragen | gültige Version `1.17.1` / `11701` |
| T04: Echter Java-Parser | Den unveränderten `UpdateManifest.parse` gegen LIVE-Dateien beider Apps ausführen | `12020 -> 12021` genau eine neue Version, `12021 -> 12021` keine |
| T05: ABI und Historie | ARM64, ARM32, x86_64 und Manifest ohne alte Versionshistorie | alle drei ABIs erhalten MarsTube-Universal-APK; fehlender alter Datensatz verhindert Aktualisierung nicht |
| T06: Produzierte APK | v0.0.20 und v0.0.21 getrennt herunterladen, SHA256SUMS und `apksigner` prüfen | gleiche Paket-ID und Zertifikat, `12021 > 12020`, beide APKs valide |
| T07: Diagnose-Skript | PowerShell-7-AST in CI parsen, nicht ausführen | keine Syntaxfehler und keine App-deinstallierenden Befehle |

## Gerätetests – erst nach bestandenem CI

| Test | Ablauf auf Fire TV / Thomson | Gesuchter Nachweis |
|---|---|---|
| D01: Realer Iststand | `adb shell dumpsys package de.equiummc.marstube2` | wirklicher `versionCode` statt nur UI-Text |
| D02: Manuelle Suche | Per TV `Einstellungen → Auf Updates prüfen`, dabei `logcat` der Updater-Tags sammeln | **wird** eine HTTP-Prüfung überhaupt gestartet? |
| D03: Serverantwort | `AppUpdates`-Protokoll mit `manifestLatest` und `manifestCode` | `12020`, `12021`, fehlende Antwort oder Abfragefehler |
| D04: Richtige Entscheidung | `manifestCode > installedCode` und resultierende Statusanzeige prüfen | bei `12021 > 12020` muss »Update verfügbar« erscheinen |
| D05: Download | erst wenn D04 bestanden ist, Update per Nutzerzustimmung laden | Fortschritt/Downloadfehler, vollständige APK |
| D06: Installer | erst nach Zustimmung des Benutzers durchführen | normales In-place-Update ohne Deinstallation oder Datenverlust |
| D07: Zukunftssicherheit | nach einer *späteren* freigegebenen Version erneut prüfen | echte 0.0.21→Folgeversion-Aktualisierung ohne ADB |

Weder D05 noch D06 laufen automatisch durch den Diagnosetest; eine App oder Einstellungen werden nicht verändert.

### Einmalige Log-Aufzeichnung

Das **lesende** Hilfsskript liegt unter [tools/Test-MarsTube-Update.ps1](https://github.com/equiummc/MarsTube/blob/diagnostics/mt2-022-update-regression-20261107/tools/Test-MarsTube-Update.ps1). Es sucht `adb.exe` zuerst lokal und lädt andernfalls die offiziellen Platform-Tools von Google in den TEMP-Ordner. Es verbindet sich nur zu einem vom Nutzer autorisierten TV, liest Paketversion und gefiltertes Android-Logcat. Es ruft **kein** `adb install`, `adb uninstall`, `pm clear` oder `logcat -c` auf.

**Fenster: separates PowerShell-7-Fenster auf Windows.** Datei von der verlinkten GitHub-Quellseite herunterladen oder per `Invoke-WebRequest` im lokalen Arbeitsordner speichern. Danach mit PowerShell 7 starten und bei Aufforderung die TV-Adresse eingeben; auf dem TV ADB-Debugging freigeben. Nun einmal im TV-Menü manuell »Auf Updates prüfen« auslösen und anschließend im Terminal ENTER drücken. Das Skript nennt den Pfad zur gefilterten Log-Datei im TEMP-Ordner.

Bei Android 14 kann **Wireless Debugging mit Kopplungscode** erforderlich sein; `adb connect` allein richtet eine solche Kopplung nicht ein. Bei Fire TV ist die ADB-Freigabe ebenfalls in den Entwickleroptionen erforderlich. Nicht autorisierte oder doppelte Geräte sind kein Testziel; der Collector bricht sicher ab.

**Auswertung:**
- `manifestCode=12021` + `up_to_date` → Fehler im App-Zustandsübergang/Versionsvergleich.
- `manifestCode=12020` → veraltete Metadaten auf dem Gerät, trotz aktueller Serverdaten; Netzwerk/Proxy/Cache untersuchen.
- `check failed` / `Error in JSON` → HTTP-/Parsingfehler, Details anhand der begrenzten Logs.
- **Kein neuer Check** im Log → TV-Einstieg, `check()`/Inflight-Guard oder Callback-Pfad prüfen.
- UI bleibt »aktuell«, aber `update available` im Log → UI-Zustands-/Lifecycle-Problem.

Alle bisherigen statischen und GitHub-Tests ersetzen **keinen** Geräte-HTTP-Trace. Erst der relevante Logwert entscheidet über die gezielte Korrektur.
