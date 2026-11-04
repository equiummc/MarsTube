# MarsTube 0.0.10 — Entwicklungsauftrag für OpenCode

Stand: 04.11.2026. Repository: `equiummc/MarsTube`. Arbeitsbranch: `feature/mt2-010-player-settings-ui-20261104`. Ausgangspunkt: `feature/mt2-009-shorts-blue-ui-20261104`.

## Sicherheit und Arbeitsweise

- Nur im 0.0.10-Branch arbeiten, nicht auf `master`, `mars/stable` oder 0.0.9.
- Keine bestehende App deinstallieren, keine Anmeldedaten/Token committen. Test-App-ID separat lassen, solange eine sichere Upgrade-Signatur nicht belegt ist.
- MarsTube 2.0 beruht derzeit auf einem gepinnten NewTube-Referenzcheckout. Unsere eigenen Overlays liegen in `mt2-player-lab/`; sie werden im CI-Workflow `.github/workflows/mt2-media3-sabr-lab.yml` in festgelegter Reihenfolge angewandt. Nicht direkt das externe Referenz-Repository als Quellwahrheit bearbeiten.
- Änderungen mit klaren Patches und automatischen Tests. Installationsskripte müssen aus vollständig entpackten Verzeichnissen laufen, Pfade zitieren, sichere Wiederholung unterstützen, und PowerShell-7-Kompatibilität besitzen.
- Alle bisherigen erfolgreichen 0.0.9-Funktionen und zügigen Videostarts schützen; insbesondere Android-TV-Geräte mit wenig RAM (Thomson ~1,45 GB, Fire TV ~2 GB).
- GitHub-Umbenennung berücksichtigen: `equiummc/MarsTube`; alte Repo-URLs und feste „0.0.9“-Verweise in neu gebauten Artefakten ersetzen.
- Für jede abgeschlossene Änderung den Nachweis festhalten: Buildstatus, Tests, veränderte Dateien, noch offene Geräteprüfungen.

## A. Navigation / TV-Fernbedienung (P0)

- BACK im laufenden Player öffnet die linke Navigationsleiste und lässt Video/Ton weiterlaufen. BACK in einem geöffneten Dialog schließt zuerst diesen Dialog. Kein Fokus-Festhängen.
- Die Steuerelemente, Navigation und Untermenüs müssen mit D-Pad vollständig erreichbar sein: links/rechts/oben/unten, OK, BACK. Seitenmenü → Inhalt per RIGHT ohne unnötiges zusätzliches OK, Rückweg aus Profil/Videoqualität/Einstellungen immer möglich.
- OK kurz: Wiedergabe/Pause; OK gehalten: temporär 2×, beim Loslassen sicher 1×; niemals als Pause-Schleife implementieren. Audio-/Decoder-Stabilität prüfen.
- Shorts: D-Pad hoch/runter zum vorherigen/nächsten Short, sofern kein Steuerelement aktiv fokussiert ist; Media Next/Previous zusätzlich; Seek-Tasten weiterhin seek. Zustandsautomat und Prioritäten explizit testen.
- Player-/Qualitätsmenüs dürfen den Fokus bei Abschnittswechseln nicht behalten oder Endlosschleifen erzeugen.

## B. Player-Design (P1)

- Referenz ist die vom Nutzer freigegebene MarsTube-Player-Grafik (dunkelblau, transparente blau getönte Steuerleiste, hochwertige Weiß/Cyan-Vektorsymbole, fokussierte Rundungen). Keine schweren Live-Blur- oder dauerhaften Animationen.
- Neue konsistente SVG/Android VectorDrawables für Like, Dislike, Play/Pause, ±10s, Previous/Next, CC, Info, HD+Zahnrad, Fullscreen. Beim HD-Symbol keine unnötigen Sternchen.
- Like/Dislike links, große Fortschrittsleiste oben über der Icon-Leiste, abgerundete Focus-States. Technische Auflösung/Codec/Bitrate nicht ständig oben anzeigen, sondern über Info-Schaltfläche.
- Textlogo „MarsTube“ in mehrfachen Headern weglassen; M-Logo bleibt.

## C. Shorts / Raster (P0/P1)

- In der Shorts-Übersicht drei vollständig sichtbare 9:16-Karten nebeneinander, sofern genügend Platz; bei kleineren Viewports auf zwei; nicht beschneiden oder verzerren.
- Auswahlrahmen genauso gerundet wie Karten. Vorschaubilder in ausreichend hoher Auflösung anfragen; keine künstliche Unscharf→Scharf-Vergrößerung beim Start.
- Player zeigt 9:16 zentriert mit Seitenbalken statt horizontaler Streckung. Bild erst bei korrektem Aspect Ratio präsentieren; bei Bedarf kurze Vorschaudeckung statt verzerrtem unscharfen Frame.
- Startgeschwindigkeit, Short-Wechsel und geringe RAM-Nutzung regressionsprüfen.

## D. Strukturierte Einstellungen (P0/P1)

Eigener Abschnitt **Einstellungen → Video & Wiedergabe** als zentraler Ort; keine doppelten/konkurrierenden Optionen. Weitere Hauptabschnitte: Darstellung, Bedienung/Fernbedienung, Untertitel, Streaming/Netzwerk, Profil/Konto, Info.

Video & Wiedergabe: automatische bzw. maximale Auflösung, max. FPS, bevorzugter Codec (Auto/H.264/VP9/AV1), HDR Auto/aus, Bitratenlimit Auto/3/5/10/15/25 Mbit/s, Puffer Klein/Normal/Groß, Video-Start (Qualität zuerst / Schnellstart). Gewählte Werte sind *Präferenzen / Obergrenzen* und dürfen nicht als garantiert angebotener Track dargestellt werden. Geräte-Decodercapabilities beachten; Default-Puffer Normal.

Streaming: SABR an/aus, Wirkung ab nächstem Video. Fallback nur bei tatsächlicher Fehlfunktion und offen sichtbar in den Videoinfos dokumentiert. Echt genutztes Protokoll und Diagnosewerte ausgeben. SABR ≠ automatische Datenersparnis.

Darstellung: wählbare Kachelanzahl auf Startseite 4/6/9, Anzahl Vorschaukarten im unteren Laufband, TV- und Smartphone-Layouts separat. Profilansicht als Raster, Verlauf vorzugsweise 3×3 bei TV, Abos ebenfalls Raster + Kanalfilter.

Untertitel: aus / nur explizit vorhandene erzwungene Übersetzungsuntertitel / reguläre Untertitel; falls forced-Tracks nicht vorhanden, keine beliebigen Untertitel aktivieren.

Downloads: auf TV nicht anbieten/anzeigen, auch wenn verborgenes Alt-Menü den Zustand ignoriert; nur auf Smartphones, sofern funktional. Miniplayer nur zeigen, wenn aktiviert und sinnvoll; bei deaktiviertem Player keine leere dauerhaft auswählbare Funktion.

## E. Architektur / Kompatibilität (P1)

- Fernseher: 16:9 D-Pad/Leanback. Smartphone: Hoch- und Querformat, Touch. Gemeinsame Player- und Einstellungslogik, unterschiedliche Oberflächen/Layouts.
- Falls neue Optionen ältere SmartTube-Einstellungen abbilden, explizite Migration/Zuordnung definieren; doppelte Menüpunkte entfernen ohne Datenverlust.
- Media3/SABR-Adapter nicht durch rein kosmetische Umbauten destabilisieren.

## F. Build / Prüfung (P0 vor Freigabe)

- Version in App/Manifest/Build sichtbar: `0.0.10` (NICHT `0.1.10`). CI-Workflow muss den neuen Branch bauen, aktuell ist er nur bis 0.0.9 verdrahtet.
- Prüfen: kompilierte APK, Signatur/Paket-ID, Größe, Geräte-Abwärtskompatibilität, Start, Shorts-Start/Wechsel, normales Video, D-Pad-Sprünge, 2×-Hold, BACK bei Video/Dialog, BITRATE-Limits, SABR an/aus und tatsächlich verwendetes Protokoll, Untertitel, TV-Downloads-Hide, Profil/Abos/Verlauf, Rückkehr aus jedem Untermenü.
- Keine „Build erfolgreich“-/„Installiert“-/„fertig“-Meldung ohne reale Ausführung. GitHub CI-Artefakte (APK, Prüfsumme) eindeutig benennen. Bei ausgebliebener Geräteprüfung diese explizit offen lassen.

## Technische Einstiegspunkte

- `mt2-player-lab/MarsTvShell.java`: Player-/D-Pad-UI
- `mt2-player-lab/MarsTvRail.java`: TV-Seitenmenü
- `mt2-player-lab/patch_009_tv_shorts_ui.py`: bisherige Shorts-Overlay-Änderungen
- `mt2-player-lab/patch_008_tv_player.py`: bisherige Player-Bedienelemente
- `.github/workflows/mt2-media3-sabr-lab.yml`: NewTube-Pin, Patch-Reihenfolge, Build, bisherige 0.0.9-Verifikation
- Vor Änderungen aktuelle Dateien vollständig lesen und tatsächliche Patch-Reihenfolge validieren.

## Lieferdefinition

Eine echte, reproduzierbar gebaute **MarsTube 0.0.10**-Test-APK samt Änderungsbericht, Git-Commit/PR und Teststatus. Die bestehenden 0.0.9-Installationen bleiben als Rückfalloption erhalten.

## G. Dauerhafte Produktziele und Sicherheits-To-dos

**Verbindliche Grundlage:** [docs/PRODUKTZIEL.md](../docs/PRODUKTZIEL.md). Diese fünf Eigenschaften müssen gemeinsam erreicht werden; ein erfolgreicher Build allein ist keine Freigabe. Die Kontrollkästchen sind bewusst offen und erst nach nachgewiesener Prüfung abzuhaken.

- [ ] **Hohe Videoqualität ohne pauschales Limit:** Automatisch die beste mit Quelle, Bildschirm, Hardwaredecoder und Netzwerk kompatible Qualität wählen; 4K/60 FPS/HDR zulassen, wenn verfügbar. 1080p oder 10 Mbit/s nicht hart als Standardlimit codieren. Manuelle Bitratenobergrenzen nur optional.
- [ ] **Schnelle, schlanke Wiedergabe:** Startzeiten, Shorts-Wechsel, Pufferverhalten, RAM- und CPU-Verbrauch auf Thomson und Fire TV messen; keine Verzerrung oder unscharfe Startbilder und keine unnötig schweren UI-Effekte.
- [ ] **Hochwertige, freigegebene Optik:** Den festgelegten neonblauen Player-Entwurf umsetzen; Transparenzen, Vektorsymbole, Steuerleiste, Fokusrahmen, Kacheln und Untermenüs durch echte Gerätescreenshots prüfen.
- [ ] **Einfache, zuverlässige Bedienung:** Fernbedienung, BACK, temporäre 2×-Wiedergabe, Shorts-Navigation, Abos, Verlauf, Profile und alle Einstellungsdialoge ohne Fokusfallen prüfen.
- [ ] **Solide App-Sicherheit:** Angriffsfläche minimieren; Android-Berechtigungen und exportierte Komponenten prüfen; TLS-/Zertifikatsprüfung, Eingabevalidierung, sichere Speicherung sensibler Daten, Geheimnisfreiheit in Git/Logs, Update-/APK-Signaturen und Herkunft der Build-Werkzeuge kontrollieren.
- [ ] **Automatisierte Sicherheitskontrollen:** Dependency-/Schwachstellenscan, Secret-Scan und Android-Lint/Manifest-Prüfung in den Entwicklungs-/Releaseprozess integrieren; Fundstellen mit Schweregrad, Maßnahme und Ergebnis dokumentieren.
- [ ] **Sichere Freigabeentscheidung:** Bekannte schwerwiegende Sicherheitsprobleme vor einem öffentlichen Release beheben oder wirksam mitigieren; APK-Build, Testergebnisse und Gerätechecks dokumentieren. Sicherheit darf nicht zugunsten der Optik oder Geschwindigkeit entfallen.

**Hinweis für OpenCode:** Das Dokument ist eine verbindliche To-do-Liste, nicht die Behauptung, die Prüfungen seien schon abgeschlossen. Während einer laufenden lokalen Entwicklungsaufgabe Änderungen von GitHub erst kontrolliert übernehmen; keinen unvorbereiteten Pull/Merge in eine bearbeitete Arbeitskopie erzwingen.
