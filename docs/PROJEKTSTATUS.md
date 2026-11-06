# MarsTube – laufender Projektstatus und Agentenübergabe

**Stand:** 04.11.2026 · **Letzter dokumentierter Scope:** MarsTube 0.0.10 / Media3 SABR-Labor

> Dieses Dokument muss nach jeder abgeschlossenen OpenCode-/Codex-Arbeitsphase fortgeschrieben werden. **Keine Prozentangaben ohne nachvollziehbare Messgrundlage.** Offene Prüfpunkte bleiben offen.

## 1. Repository und aktiver Entwicklungsstand

- Repository: `equiummc/MarsTube`
- 0.0.10-Entwicklungsbranch: `feature/mt2-010-player-settings-ui-20261104`
- 0.0.9-Ausgangsbranch: `feature/mt2-009-shorts-blue-ui-20261104`
- Technischer Stand: NewTube-Referenz @ `6f96a1b42bb9272935360d7e5aec34d35f2753f0`, MarsTube-Overlay-Pipeline; Media3 1.10.1 laut Referenzprojekt
- **Die vollständige technische Eigenständigkeit steht noch aus.** Künftig Stable und Alpha als zwei getrennt installierbare APK-Kanäle aus gemeinsamer Basis.

## 2. Nachweise (historisch, keine Freigabe)

| Teil | Befund | Beleg / Einschränkung |
| --- | --- | --- |
| 0.0.10 Android-Build | Lokal erfolgreich | Nutzerausgabe `mt2-player-lab/build-010-local.log`: `BUILD SUCCESSFUL in 57s`, 241 Gradle-Aufgaben |
| Unit-Test-Task | Ausgeführt ohne gemeldeten Fehlschlag | `testStmobileDebugUnitTest` im Buildlog; Anzahl/Aussagekraft einzelner Tests offen |
| APK erstellt | Durch `assembleStmobileDebug` impliziert | Paketkennung, Signatur und exakter Pfad noch nicht nachgewiesen |
| Geräteprüfung Fire TV | **Offen** | Kein Testbericht für neue 0.0.10 vorliegend |
| Geräteprüfung Thomson | **Offen** | Kein Testbericht für neue 0.0.10 vorliegend |
| Übereinstimmung mit freigegebenem Zielbild | **Offen** | Freigabevorlage muss als Datei eingecheckt werden |
| SABR mit Media3 1.11.1 | **Nicht integriert/nicht getestet** | Stable-Upgradeziel |
| SABR mit Media3 1.12.0-alpha02 | **Nicht integriert/nicht getestet** | Alpha-Upgradeziel |
| OpenCode-Abschluss / Reproduktions- und Testdokumentation | **Lokal vom Nutzer als abgeschlossen gemeldet** | Genaue Abschlussdateien, Testresultate und GitHub-Commit noch zu prüfen; nicht mit freigegebenem Produkt gleichsetzen |

## 3. Aktuell priorisierte To-dos

- [ ] **OpenCode-Handoff sichern:** Tatsächliche lokale Abschlussdateien, Build-/Testlogs, Änderungen, Branch/Commit und erzeugte APK prüfen; sicher und ohne Datenverlust nach GitHub übertragen.
- [ ] **Freigegebenes MarsTube-Zielbild als Datei** ins Repository nehmen und Referenzpfad in den Entwicklungsdokumenten ergänzen.
- [ ] **0.0.10 auf Geräten testen:** Navigation, Back/OK-hold, Shorts 9:16, Qualitätsmenüs, Infoanzeige, Untertitel, ABR, SABR/DASH/HLS, Ladezeiten, RAM.
- [ ] **Stable/Alpha-Kanäle vorbereiten:** getrennte App-IDs/Signaturen, ein Media3-Player pro APK, keine versehentlichen Überschreibungen.
- [ ] **Media3-Upgrades isoliert:** Stable-Kandidat 1.11.1; Alpha-Kandidat 1.12.0-alpha02. SABR-Brücke nach jedem Upgrade kompilieren und Video/Untertitel auf Geräten verifizieren.
- [ ] **SABR-Stabilität erhöhen:** Netzwerk- und Protokollfehler trennen, begrenzte Retries nur bei transienten Störungen, saubere Rückkehr zu verfügbaren kompatiblen Streams, keine Endlosschleifen; reproduzierbare Fehler-/Seek-/Shorts-Tests.
- [ ] **Eigene Buildbasis:** Externen vollständigen App-Checkout durch eigene versionierte Android-Codebasis ersetzen; Lizenzen und Provenienz bewahren.
- [ ] **Sicherheit:** Android Manifest/Berechtigungen, Signaturen, verwendete Abhängigkeiten, sichere Netzwerkpfade und Logs prüfen; kritische Funde vor öffentlichem Release beseitigen.

## 4. Standard-Übergabeformat nach jeder Agentenarbeit

Jeder Agent **ergänzt** hier:

### Übergabe — [Datum/Uhrzeit] — [Agent] — [Auftrag/Branch]

- **Stand:** Fertig / Teilweise / Blockiert (jeweils begründen)
- **Commit(s):** tatsächliche SHAs oder „nur lokale Änderungen“
- **Geänderte Dateien und Funktion:** Dateipfade + kurze Wirkung
- **Build:** exakter Befehl/Variante, erfolgreich/fehlgeschlagen, Protokollpfad
- **Tests:** Namen, Ausführungsnachweis, Anzahl/Ergebnisse; **Gerätetests separat**
- **APK:** tatsächlicher verifizierter Pfad, App-ID, Version, Signatur, Prüfsumme oder „noch nicht belegt“
- **Bekannte Fehler / Risiken:** reproduzierbarer Befund, betroffener Bereich
- **Nächster Schritt:** gezielte konkrete Aufgabe, keine pauschale neue Gesamtanalyse

**Wichtig:** Eine aktualisierte Datei auf GitHub wird nicht automatisch in eine bereits laufende lokale OpenCode-Sitzung übertragen. Änderungen nach Arbeitsabschluss kontrolliert mit dem lokalen Git-Zustand abgleichen.

### Fire-TV-Gerätetest 0.0.10 – neue offene P0-Fehler

**05.11.2026, tatsächlicher Nutzerbefund auf Amazon Fire TV Cube (AFTR):**

- [ ] BACK aus Player/Navigation verhält sich nicht zuverlässig.
- [ ] Shorts D-Pad nach unten wechselt in ein normales Video statt zum nächsten Short im selben Feed.
- [ ] Player-Seitenmenü ist gegenüber der schmalen Hauptnavigation zu breit.
- [ ] **Miniplayer P0:** Läuft nach Verkleinern rechts als überlagerndes Fenster weiter, lässt sich mit der TV-Fernbedienung nicht zuverlässig anwählen/vergrößern und verdeckt Bedienbereiche. Optionaler Miniplayer bleibt Produktziel, muss bei aktivem Video eindeutig fokussierbar sein; OK zurück zu Vollbild, auf allen Browse-/Suchseiten erreichbar, deaktivierbar. Keine zweite Playerinstanz.

**Fachlicher Fixauftrag:** [MT2-010R-FIRETV-ABNAHME.md](../mt2-player-lab/MT2-010R-FIRETV-ABNAHME.md). Fehler sind auf Geräten beobachtet und analysiert, **noch nicht behoben oder auf Fire TV/Thomson nachgetestet**. Keine allgemeine 0.0.10-Freigabe.

### MT2-011 – integrierter Korrekturzweig

**05.11.2026 – implementiert, realer Gerätetest weiterhin offen.** GitHub-Branch: `feature/mt2-011-firetv-controls-navigation-20261105`; Basis `feature/mt2-010-player-settings-ui-20261104` (0.0.10 bleibt erhalten).

- [x] **Neuer isolierter Android-Paketstand vorbereitet:** `de.equiummc.marstube2.firetest011.debug`, `versionCode 12011`, `versionName 0.0.11`; reproduzierbarer neuer Patch `mt2-player-lab/patch_011_firetv_navigation.py` nach dem 0.0.10-Overlay in CI.
- [x] **Playerleiste im Quellcode** umgebaut: zentrierte weiße Vektorsymbole, cyanblaue leichte Verlaufsflächen, abgerundete Schaltflächen/Fokus, schmale dauerhafte TV-Rail mit Icons statt 240-dp-Textleiste. Stil orientiert sich am freigegebenen Zielbild; **visuelle Identität noch nicht durch Geräte-Screenshot bestätigt**.
- [x] **BACK-Logik im Code** über dieselbe Shell-State-Machine für Back-Event und Android-Callback; BACK bei Rail dockt laufenden Player in den Mini-Host, ohne Neu-Player.
- [x] **Shorts-Queue** `MarsShortsSession.java` als abgegrenzte Sammlung aus dem echten `TYPE_SHORTS`-Feed statt generischer Empfehlung oder bloßer Hochformatprüfung. Hoch/Runter + Media Next/Previous bleiben im Feed; am Ende kein automatischer Sprung zum normalen Video. Test `MarsShortsSessionTest.java` hinzugefügt.
- [x] **Mini-Zugriff im Code**: dynamischer Eintrag `Laufendes Video` nur bei aktiver Mini-Wiedergabe; mit OK wieder vergrößern, Fokusrahmen; rechter Bereich des Browse-Rasters bei aktivem Mini freigehalten. Vollständige Funktion auf Suche und anderen TV-Seiten sowie echtes Gerätesichtbild **noch offen**.
- [x] **Erster GitHub-CI-Build** ohne nachträglich ergänzte Shorts-Tests erfolgreich: Run [41937350004](https://github.com/equiummc/MarsTube/actions/runs/41937350004). Der neueste Build nach dem Test-/Shorts-Feed-Fix ist separat nachzuweisen.
- [x] **Erster erweiterter Shorts-Testlauf:** [41937721476](https://github.com/equiummc/MarsTube/actions/runs/41937721476) ergab 13 Tests, davon 1 fehlerhafte Erwartung in `nextAndPreviousStayWithinActualShortsFeed`; Testdaten am 05.11.2026 korrigiert, nicht als bestandener Lauf behaupten.
- [ ] **Aktueller CI-Build einschließlich korrigierter Shorts-Tests und dynamischer Versionsanzeige:** [41938747357](https://github.com/equiummc/MarsTube/actions/runs/41938747357) – Ergebnis bei Eintrag noch offen.
- [ ] **Fire TV Cube / Thomson:** vollständiger sichtbarer Funktionstest für BACK, Shorts, Miniplayer, Bedienknöpfe, Vorspulen/2×, Menü/Focus, Speicher und Stabilität.
- [ ] **Visuelle Abnahme:** Vergleich der echten Fire-TV-Oberfläche mit dem vom Nutzer freigegebenen neonblauen Zielbild. Das Originalbild muss noch als versionierte Referenzdatei aufgenommen werden.

**Installationshelfer:** `tools/Install-MarsTube-011-FireTV.ps1` (im PowerShell-7-Terminal mit explizitem APK-Pfad). Bestehende Testpakete 0.0.9 und 0.0.10 nicht entfernen.

### MT2-012 – Updatefunktion direkt auf Android TV

**05.11.2026 – in der Entwicklung, noch keine freigegebene TV-APK.** Branch `feature/mt2-012-inapp-updates-20261105` auf Basis 0.0.11.

- [x] Neuer isolierter Build vorbereitet: Version `0.0.12`, Code `12012`, Paket `de.equiummc.marstube2.firetest012.debug`.
- [x] Echter TV-Info-Menüpunkt „Auf Updates prüfen“ startet vorhandene Updateoberfläche mit D-Pad-Fokus und Bestätigungsdialog; alte Dummy-Aktion entfernt.
- [x] **Fremde Updatequelle entfernt:** Ausschließlich `https://github.com/equiummc/MarsTube/releases/latest/download/marstube-tv.json`, Debug-Systemproperty als Umleitungsweg gesperrt.
- [x] `MarsUpdateApkGuard` prüft APK-Datei, unveränderte Paket-ID, steigenden Versionscode und identische Signatur vor Android-Installationsdialog. `REQUEST_INSTALL_PACKAGES` mit CI-Sicherheitsausnahme für explizites Update.
- [x] CI-Build **[41941226412](https://github.com/equiummc/MarsTube/actions/runs/41941226412)**: **erfolgreich**; SABR-/Media3-Prüfung, TV-Einstellungen, Shorts-Tests, neue APK-Guard-Tests und Berechtigungsprüfung bestanden. Testartefakt [marstube-v0.0.12-firetv-inapp-updater-debug](https://github.com/equiummc/MarsTube/actions/runs/41941226412/artifacts/13375973504). **Noch keine Geräteabnahme und keine dauerhaft signierte Updatebasis.**
- [ ] **Dauerhafte Signatur** einrichten, bevor 0.0.12 als echte Updatebasis installiert wird. Die normalen CI-Debug-Schlüssel wechseln zwischen Läufen; diese APKs sind keine zuverlässige Updatekette.
- [ ] Eigenes GitHub Release mit signierter höherer Version und validem Update-Manifest veröffentlichen. Bis dahin zeigt Updateprüfung ggf. einen Fehler (kein Feed verfügbar).
- [ ] Reale Geräteabnahme der Updateoberfläche/Installer-Berechtigungen auf Fire TV Cube und Thomson; 0.0.12 → 0.0.13 tatsächlich testen, ohne Einstellungen zu verlieren.
- [ ] Sicherheitsprüfung um SHA-256-Abgleich mit vertrauenswürdigem Release-Manifest ergänzen, bevor öffentlich ausgerollt wird.

Details: [MT2-012-UPDATE-FUNKTION.md](MT2-012-UPDATE-FUNKTION.md), GitHub [Issue #38](https://github.com/equiummc/MarsTube/issues/38).

### MT2-013 – TV-Fokus, schlanke Leiste und Update-Lab

**05.11.2026, Nutzerabnahme v0.0.12: fehlgeschlagen.** Tatsächlich beobachtet: Rail und Einstellungsnavigation zu breit, Player-Rail dauerhaft eingeblendet, Buttons deutlich zu groß, Shorts-Kachel nicht mit Fernbedienung erreichbar, BACK ohne erwartete Wirkung.

**Implementiert in Code (Gerätetest offen):** Rail auf 64dp, Beschriftungen gegen Icons ohne breite Textkacheln; Player-Rail nur bei Fokus sichtbar; Buttons von 48/58dp auf 32/38dp; ein BACK aus dem Player zurück zu Browse (Info zuerst schließen); Shorts-Fokus ab Rail rechts auf erstes Video, inkl. begrenztem Nachladen; BACK bei leerem Shorts-Feed ebenfalls möglich. Änderungen: `MarsTvShell.java`, `MarsTvRail.java`, `patch_013_tv_focus_ui.py`.

**Separate Prüfpfade:** Eigenständige `0.0.13`-APK mit `de.equiummc.marstube2.firetest013.debug`; plus echtes 0.0.12→0.0.13-Testpaar mit festem Lab-Paket `de.equiummc.marstube2.updatelab.debug` und identischer temporärer Signatur **innerhalb eines CI-Runs**. Bisherige 0.0.12 mit anderer Signatur nicht direkt updatefähig. Alte Installation niemals ungefragt löschen.

- [ ] Normaler 0.0.13-CI-Lauf: [41944918270](https://github.com/equiummc/MarsTube/actions/runs/41944918270), Ergebnis noch offen.
- [ ] Paar-Build/Prerelease: [41944917528](https://github.com/equiummc/MarsTube/actions/runs/41944917528), Ergebnis noch offen. Publikation ausschließlich als isoliertes GitHub-Prerelease, **nicht** als Stable/latest.
- [ ] Bedienung, Shorts, BACK, Miniplayer und UI **real am Fire TV Cube**, danach Thomson testen; weder CI-Erfolg noch Quellcodeänderung sind bereits ein Gerätetest.
- [ ] Einmalig gepaarte v0.0.12-Lab-Basis per ADB installieren → v0.0.13 **über TV-Updatefunktion** abrufen; Zustimmung im Android-Installer, Versionswechsel und Datenbestand prüfen.
- [ ] Dauerhafter Signaturschlüssel für spätere echte Updates; Testschlüssel pro Lab-CI nicht über mehrere Builds stabil.

Details: [MT2-013-ABNAHME.md](MT2-013-ABNAHME.md).


### MT2-017 – Shorts-Split-Screen und durchgehende TV-Fernbedienung

**06.11.2026 – implementierter Codeentwurf; keine Gerätefreigabe.**

- **Branch:** `feature/mt2-017-shorts-stage-20261106`, erstellt vom Diagnosezweig `feature/mt2-017-stream-recovery-20261106`; die zuletzt funktionierende App 0.0.16 wurde nicht verändert.
- **Quellstand:** `mt2-player-lab/MarsTvShortsStage.java` (vier Vorschauen links, Uhr, blaues Glasdesign, rechts 9:16-Short, Timeline und Auto-Fortsetzung), `MarsShortsSession.java` (auf echte Shorts beschränkte Vorschau- und Queue-Ende-Methoden), `MarsTvShell.java` (D-Pad-Beschleunigung beim Halten, vorhandene BACK-Logik, automatisches Weiter ohne normales Video).
- **Overlay & UI:** `patch_017_shorts_stage.py` (einmaliger Sofortstart auf Shorts-Seite, Positionierung des **bestehenden** Media3-Views, verzögerter Ladeindikator nach 5 Sekunden), `MarsTvNavigationTheme.java` und `assets/drawable/mars_tv_ic_nav_*.xml` (neue Kontur-Icons).
- **Version:** nur als Buildkandidat `0.0.17`, Code `12017`, Paketkennung **unverändert** `de.equiummc.marstube2`. Produktives Release mit identischem Signierschlüssel notwendig; kein Side-by-Side-Testpaket.
- **Quelltests:** `MarsShortsSessionTest.java` erweitert um Vorschau/Ende/Leerlauf. `.github/workflows/mt2-017-shorts-ci.yml` kompiliert und prüft ohne Release; **Ergebnis gesondert nachtragen**. Aus einem laufenden CI-Job darf kein Erfolg abgeleitet werden.
- **Offene Punkte:** GitHub-CI erfolgreich abschließen, Paket-Signierung und reale OTA-Aktualisierung belegen, Fire TV Cube und Thomson auf Darstellung, Fokus, Rücktaste, Spulen, Shorts Auto-Auswahl, Decoder, Startzeiten, RAM und Spinner testen. 0.0.16 bis dahin behalten.
- **Spezifikation:** [MT2-017-SHORTS-STAGE.md](MT2-017-SHORTS-STAGE.md). Der frühere Videostream-Ausfall trat laut Nutzer plötzlich nicht mehr auf; Ursache unbestätigt, weder SABR noch Netzwerk als definitiv behoben markieren.


## MT2-019 – Shorts im festen Drei-Spalten-Layout (06.11.2026)

- Arbeitsbranch: `feature/mt2-019-shorts-three-column-20261106`, Ausgangsstand `feature/mt2-018-shorts-miniplayer-ux`. Nicht auf `master` oder `mars/production-2` entwickelt.
- Version: `0.0.19`, `versionCode=12019`, **unveränderte** Paketkennung `de.equiummc.marstube2`. Produktives Android-Update benötigt weiterhin denselben privaten Signaturschlüssel und ein geprüftes GitHub-Release mit `marstube-tv.json`; Debug-APK ist kein sicheres In-place-Update.
- Links: exakt drei größere Vorschaubilder in Hochkantdarstellung und mehrzeilige Titel; technische Vorschauliste bleibt selektiv und puffert keine drei kompletten Videos.
- Mitte: Branding SHORTS und kleinere Uhr oben, kompakter Auto-EIN/AUS-Schalter, eine Laufzeitkapsel mit Trennstrich, vertikale gelbe Symbolaktionen (Like, Dislike, echtes Speichern in YouTube-Playlist).
- Rechts: ein vorhandener Media3-Player mit festem 9:16-Fenster bis zum rechten Rand. Auch breitere Videos werden per ZOOM seitlich beschnitten. Der 7-dp-Balken sitzt direkt darunter an der unteren Bildschirmkante und verdeckt kein Videobild.
- D-Pad: Vorschau → mittlere Aktionen → Video; Links aus der Vorschau öffnet die vorhandene Rail. UP/DOWN im Videofokus wechselt Shorts. Richtungstasten an fokussiertem Fortschrittsbalken und dedizierte Medien-Spultasten: Stage-Seek bei Halten, einmaliger Commit beim Loslassen. OK lang: 2× im Videofokus wie zuvor. PLAY/PAUSE ergänzt.
- Unverändert: BACK über bestehende Rail, vorhandene Wiedergabe- und Netzwerkpfade, Autoplay nach tatsächlichem Shorts-Ende, 5-Sekunden-Ladespinner, TV-System-PiP-Sperre, Ein-App-Updateprüfung und Signaturprüfung.
- Geändert: `mt2-player-lab/MarsTvShortsStage.java`, `MarsTvShell.java`, `patch_019_shorts_three_column.py`, `production-version.json`, `test_019_shorts_contract.py`, `tools/build-marstube-production.sh`, `.github/workflows/mt2-019-shorts-ci.yml`.
- **Prüfung (Stand GitHub-Änderung):** Statische Vertragsprüfung im CI vorgesehen, Android-Java-Compile/Unit-Test/Debug-Build angestoßen. Reale Fire-TV- und Thomson-Tests, Fokusprüfung, Cropping an breiten Shorts und produktive Updateinstallation **noch offen**. Ohne nachweislich grüne CI keine Freigabe.

- **CI-Befund vom 06.11.2026:** Pinned Checkout, komplette Patch-Pipeline inklusive Patch 019, Vertragsprüfungen, Android-Java-Compile, `:smarttubetv:testStmobileDebugUnitTest` und `:smarttubetv:assembleStmobileDebug` **erfolgreich**. Nachweis: https://github.com/equiummc/MarsTube/actions/runs/42298137973. Dies ist noch kein signierter Produktions-Build oder realer TV-Test.
- Konfliktfreier Produktions-PR: https://github.com/equiummc/MarsTube/pull/54 (squash-freundlich, nur acht geänderte Dateien auf 0.0.18-Produktions-HEAD).


## MT2-020 – Updateprüfung: Frische Metadaten statt falscher „aktuell“-Meldung (06.11.2026)

- Nutzerbild mit **installierter 0.0.18** zeigt in der Updateprüfung **„MarsTube ist aktuell“**, obwohl GitHub `v0.0.19` mit APK, Manifest und SHA-256 am 06.11.2026 um 21:52 UTC veröffentlicht hat. Fehler tritt **vor Download/PackageInstaller** auf. Aktueller genauer Grund auf Gerät (HTTP-Cache, CDN-Redirect oder konkurrierender Auto-Check) ohne HTTP-Trace noch nicht bewiesen.
- `patch_020_update_freshness.py` patcht den in der gepinnten externen SharedModules-Unterkomponente vorhandenen `AppVersionChecker`: Metadaten-Abfragen erhalten einen eindeutigen `mars_check`-Queryparameter aus Uhrzeit und monotonic nanos. APK-Download-URLs und Signaturprüfung bleiben unverändert.
- In `AppUpdates` führt eine manuelle Anfrage während eines laufenden Autochecks nach dessen Antwort eine neue HTTP-Abfrage aus. Log erfasst verarbeitete `manifestLatest`-/`manifestCode`-Werte und installierte Version. Keine stillschweigende Gleichsetzung von automatisch zuletzt ermittelter Antwort und neuer Nutzerprüfung.
- Die Info-Seite sagt nun „reguläre MarsTube-App“ statt historisch falschem „separate Test-App“; Paketkennung bleibt `de.equiummc.marstube2`.
- Versionsvertrag: **0.0.20 / 12020**. Neues Release darf nur mit derselben permanenten Besitzer-Signatur über `mars/production-2` gebaut werden, niemals mit dem internen Compile-Signaturschlüssel.
- Änderungen: `mt2-player-lab/patch_020_update_freshness.py`, `production-version.json`, `MarsTvSettingsPages.java`, `test_020_update_contract.py`, `tools/build-marstube-production.sh`, `.github/workflows/mt2-020-update-ci.yml`.
- **Prüfstatus:** CI-Compile und Tests laufen über isolierten Featurebranch; realer HTTP-/Installationsnachweis auf Fire TV / Thomson weiterhin ausstehend. Vor diesem Nachweis bitte nicht behaupten, der Update-End-to-End-Weg funktioniere auf beiden Geräten.
