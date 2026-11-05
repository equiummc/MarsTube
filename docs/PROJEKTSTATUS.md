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
- [ ] **Neuester CI-Build einschließlich Shorts-Tests:** [41937721476](https://github.com/equiummc/MarsTube/actions/runs/41937721476) – Ergebnis bei Eintrag noch offen. Ergebnis/Artefakt nach tatsächlicher Prüfung ergänzen.
- [ ] **Fire TV Cube / Thomson:** vollständiger sichtbarer Funktionstest für BACK, Shorts, Miniplayer, Bedienknöpfe, Vorspulen/2×, Menü/Focus, Speicher und Stabilität.
- [ ] **Visuelle Abnahme:** Vergleich der echten Fire-TV-Oberfläche mit dem vom Nutzer freigegebenen neonblauen Zielbild. Das Originalbild muss noch als versionierte Referenzdatei aufgenommen werden.

**Installationshelfer:** `tools/Install-MarsTube-011-FireTV.ps1` (im PowerShell-7-Terminal mit explizitem APK-Pfad). Bestehende Testpakete 0.0.9 und 0.0.10 nicht entfernen.
