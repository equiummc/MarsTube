# MarsTube – Eigenständigkeit und Media3-Upgrade-Roadmap

Stand 04.11.2026 · Repository: equiummc/MarsTube · Aktueller Arbeitszweig: feature/mt2-010-player-settings-ui-20261104

## Produktentscheidung

MarsTube ist ein **eigenständiges Produkt** mit eigener Marke, Architektur, Benutzeroberfläche, Qualitätsautomatik, Geräteunterstützung und Release-Pipeline. Öffentliche Benutzeroberflächen, Menüs, Update- und Installer-Texte sollen konsequent „MarsTube“ verwenden.

Rechtlich notwendige Open-Source-Lizenzhinweise, Copyright-Vermerke und Software-Notices bleiben erhalten. Bei der Nutzung von Inhalten bzw. Diensten Dritter bleiben deren technische Zugriffsanforderungen und Nutzungsbedingungen relevant; eigenes Branding ändert dies nicht.

## To-do: technische Unabhängigkeit

- [ ] Nach Fertigstellung von 0.0.10 funktionierenden Zustand und Tests sichern. Kein Umbau des laufenden OpenCode-Auftrags.
- [ ] Inventar: welche eigenen MarsTube-Klassen, externen Module, Patches, Submodule, Ressourcen und Buildskripte werden wirklich verwendet?
- [ ] Eigenes reproduzierbares MarsTube-Android-Projekt anlegen, das ohne automatischen Checkout einer externen vollständigen App baut. Noch verwendete Bibliotheken als deklarierte, nachvollziehbare Abhängigkeiten einbinden; eigenentwickelte Komponenten direkt im eigenen Repository verwalten.
- [ ] Die aktuellen Overlay-Skripte aus mt2-player-lab schrittweise durch reguläre Module/Quellen und Integrationstests ersetzen. Erst nach nachgewiesener Funktionsparität entfernen.
- [ ] Begriffe/Branding der ursprünglichen Apps aus UI, Manifest, Installer, APK-Namen, Ressourcen und Release-Kommunikation entfernen; nicht blind Java-Paketnamen, Signaturen, gespeicherte Benutzerprofile oder API-Bezeichner ändern.
- [ ] Herkunft, Lizenzen und Sicherheits-/Update-Historie intern dokumentieren. Notwendige rechtliche Notices unverändert ausliefern.
- [ ] Bestehende 0.0.9/0.0.10-Installationen nicht deinstallieren oder überschreiben; Paket-ID/Signaturen und Migrationsweg getrennt verifizieren.

## Media3: Entscheidung vom 04.11.2026

Offizieller Stand: **1.11.1 stabil**, **1.12.0-alpha02 Vorabversion** (veröffentlicht 26.10.2026); bisheriger gepinnter Referenzstand nutzt **1.10.1**.

**Hauptlinie:** Nach dem stabilen 0.0.10-Build Media3 auf 1.11.1 in einem separaten Branch migrieren und umfassend testen.

**Experiment:** 1.12.0-alpha02 in einem **isolierten Testbranch** prüfen, nicht automatisch in die normale Version übernehmen. Relevante potenzielle Vorteile der 1.12er-Alphas:
- Fix für Preload-Memory-Leak und ausstehende Preload-Rennen; relevant für Shorts.
- Fix für Audio-Freeze auf Amazon Fire TV bei AC-3-Periodenwechseln.
- Trackwahl-Fix für unterstütztes Dolby Vision gegenüber SDR.
- Fix für stockende Wiedergabe bei AudioTrack-Playbackhead-Reset und DASH-Multi-Period-Ladefehler.
- Neue Track-Ladepolitik kann Speicher sparen, aber beim späteren Zuschalten unselektierter Videotracks zu zusätzlichem Puffern führen.
- Display-P3-Metadaten; nicht gleichzusetzen mit garantierter HDR-/4K-Unterstützung.
- Neues optionales Modul für Play-Services-basierte Videoeffekte und Audio-Separation. Keine automatische Nutzung; keine zusätzliche Pflichtabhängigkeit auf schwachen TVs/Fire TV.

**Risiken:** Alpha-APIs können sich bis Stable ändern, darunter geänderte/entfernte experimentelle APIs und Compose-Schnittstellen. Der derzeitige Media3-SABR-Adapter greift auf versionsspezifische Schnittstellen zurück: insbesondere die Brücke SabrSubtitleSourceFactory (package-private-gebundene Funktionalität) nach jedem Upgrade kompilieren, Untertitel und reale Streamwiedergabe prüfen.

### Testmatrix vor Upgrade-Freigabe

- [ ] Reproduzierbarer Build; alle Media3-Artefakte konsistent gleiche Version.
- [ ] DASH, SABR an/aus, sichtbarer Fallback und reale Streamidentifikation.
- [ ] Normale Videos/Shorts: Startzeit, erster scharfer Frame, Seek, Vor/Zurück, häufige schnelle Wechsel, fehlende Schwarzbilder.
- [ ] Auflösung 1080p/4K soweit vom Gerät unterstützt, 30/60 FPS, VP9/AV1, SDR/HDR/Dolby Vision soweit Quelle und Hardware es anbieten.
- [ ] Audio (inkl. AC-3, wo vorhanden), 2×-Hold, Untertitel, Audio-/Trackwechsel.
- [ ] Puffer, Speicherverbrauch und Hänger bei längerer Laufzeit; Thomson (wenig RAM), Fire TV, Sony.
- [ ] Sicherheits-/Lizenz-/APK-Signatur-Review; vorhandenen stabilen Branch erhalten.

**Entscheidungsregel:** Eine neue Versionsnummer ist kein Qualitätsbeweis. 1.12-alpha02 nur übernehmen, wenn für MarsTube messbar besser und ohne Regressionen; sonst 1.11.1 stabil vorziehen.

Quelle: https://developer.android.com/jetpack/androidx/releases/media3
