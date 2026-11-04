# AGENTS.md — MarsTube: zentrale Entwicklungsregeln

**Gültig für dieses gesamte Repository.** Dieses Dokument zuerst lesen, **vor** Codeanalyse, Änderungen, Build oder Commit. Es gilt für OpenCode, Codex und andere Coding-Agenten.

## 1. Projektidentität und Dokumentationspflicht

- **Produkt:** MarsTube (M und T groß), eine eigenständige Android-TV-/Fire-TV- und künftig Smartphone-Streaming-App. Nicht als NewTube, SmartTube oder altes Fork-Projekt präsentieren.
- **Repository:** `equiummc/MarsTube`; Windows-Arbeitskopie des Nutzers: `E:\MarsTube`.
- **Zuerst lesen:** [docs/PROJEKTKONTEXT.md](docs/PROJEKTKONTEXT.md), [docs/PROJEKTSTATUS.md](docs/PROJEKTSTATUS.md), [docs/PRODUKTZIEL.md](docs/PRODUKTZIEL.md), [mt2-player-lab/MT2-010-ARBEITSAUFTRAG.md](mt2-player-lab/MT2-010-ARBEITSAUFTRAG.md) und [docs/MARSTUBE-EIGENSTAENDIGKEIT.md](docs/MARSTUBE-EIGENSTAENDIGKEIT.md).
- **Zielbild verbindlich:** Vom Nutzer am 04.11.2026 freigegebenes neonblaues TV-Player-Design. Das tatsächliche Bild muss noch als Referenzdatei ins Repository aufgenommen werden; **keinen neuen oder nur ähnlichen Entwurf als freigegeben ausgeben**.
- **Am Ende jeder Arbeitsphase:** `docs/PROJEKTSTATUS.md` mit erledigten Punkten, Dateipfaden, Commit/Branch, Build-/Testergebnissen, Fehlern und nächsten Schritten aktualisieren. Bei Architekturänderungen auch `docs/PROJEKTKONTEXT.md` aktualisieren. Nichts als erledigt markieren, was nicht nachweislich geprüft wurde.

## 2. Technische Wahrheit, nicht historische Namen

- **Der aktuelle 0.0.10-Build** nutzt noch ein **fest gepinntes externes NewTube-Quellprojekt** mit MarsTube-Patches aus `mt2-player-lab/`. Das ist eine **technische Übergangslösung**, keine fertige eigenständige Codebasis.
- **Build-Wahrheit:** `.github/workflows/mt2-media3-sabr-lab.yml` (Reihenfolge, Quellstand, Tests); Änderungen an Python-Overlays dürfen die Reproduzierbarkeit nicht zerstören.
- **Keine unkontrollierten Änderungen** in `.mt2-build/`, Gradle-Caches oder externen Klonen als endgültige Quelle. Eigene Änderungen im versionierten MarsTube-Code hinterlegen.
- **Geplante Migration:** eigenständiges Android-Projekt ohne Pflicht-Checkout einer anderen Gesamt-App; bei Übergängen Systemtests und Lizenzen sichern.
- **Stable/Alpha sind zwei geplante App-Kanäle**, nicht bereits nachweislich fertiggestellte Releases. Stable: Media3 1.11.1 als Prüfziel; Alpha: Media3 1.12.0-alpha02 als isolierter Prüfzweig. **Je APK eine Media3-Version**, gemeinsame MarsTube-Logik; getrennte App-ID, Daten und signierte Builds nach Prüfung. Der bestehende Referenzstand nutzt Media3 1.10.1. Kein ungeprüfter Player-/Versionswechsel.
- **SABR** ist ein zusätzlicher Adapter, nicht in Media3 integriert. Separate Tests für SABR, DASH/HLS, Fehlerwiederherstellung, Untertitel, Fallback, Seek und Trackwechsel. Siehe `docs/MARSTUBE-EIGENSTAENDIGKEIT.md`.

## 3. Verbindliche Qualität

- Kein künstliches Standardlimit auf 1080p, 10 Mbit/s oder 30 FPS. 4K, 60 FPS, HDR und passende Codecs dort anbieten, wo Quelle, Verbindung und Gerätehardware es ermöglichen.
- Schneller erster **scharfer, unverzerrter** Frame; Shorts im 9:16-Format; keine schwarzen Wechsel, Audioaussetzer oder blockierten Einstellungsmenüs.
- Leichtgewichtig für Thomson Android TV (~1,45 GB RAM) und Fire TV (~2 GB RAM); ressourcenintensive Dekoration vermeiden.
- Fernseher vollständig per D-Pad/OK/BACK bedienbar. OK kurz Play/Pause, lang 2× nur während des Haltens; BACK zur Navigation, ohne unnötigen Wiedergabestopp.
- Sicherheit als Freigabekriterium: Berechtigungen/Export-Komponenten, TLS, saubere URL-/Eingabeprüfung, Signatur/Updateherkunft, Dependency-/Secret-Scan, sensible Logs vermeiden. Fremde Lizenz- und Copyright-Hinweise **erhalten**, auch wenn das sichtbare Produkt nur MarsTube heißt.

## 4. Sichere Zusammenarbeit mehrerer Agenten

1. **Vor jeder Änderung:** `git status`, Branch und vorhandene uncommittete Arbeiten prüfen. Bei fremden parallelen Änderungen nicht einfach resetten, überschreiben, wechseln, mergen oder pullen.
2. Kurz Arbeitsplan, betroffene Dateien und erwartete Prüfungen notieren; **nur relevante Dateien** öffnen, keine pauschale Neuanalyse des kompletten Android-Repositories.
3. **Implementierung reproduzierbar** mit Tests; alte funktionierende App/Signatur nicht ohne ausdrücklichen Auftrag deinstallieren oder ersetzen.
4. **Build-Erfolg ≠ Geräte-Erfolg.** Ein erfolgreicher Gradle-Build beweist keine 4K-/SABR-/Player-Stabilität. Echte Tests als offen dokumentieren, bis sie auf dem Gerät ausgeführt wurden.
5. **Keine Geheimnisse** (Tokens, Cookies, Zugangsdaten, URLs mit Signaturen), Download-Artefakte oder Builds/Caches committen. Keine ungesicherten Installations- oder Ausführungsanweisungen.
6. Bei PowerShell-Befehlen stets **das genaue Fenster** nennen (z. B. VS Code → integriertes PowerShell-7-Terminal); kurze, geprüfte, kopierbare Befehle bevorzugen.
7. Bei Abschluss: Änderungen und Belege knapp berichten; `docs/PROJEKTSTATUS.md` aktualisieren; keine fiktiven Tests, APKs oder Prozentwerte.

## 5. Vorrang bei Widersprüchen

Für **Produktziele** gilt `docs/PRODUKTZIEL.md`; für den aktuellen Arbeitsumfang der konkrete freigegebene Auftrag; für **tatsächlich gebaute Versionen** der reproduzierbare Build und seine Testprotokolle; für offene Punkte `docs/PROJEKTSTATUS.md`. Historische, unveränderte Alt-README-Texte sind **keine** aktuelle MarsTube-Spezifikation.
