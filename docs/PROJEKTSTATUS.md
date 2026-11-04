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
