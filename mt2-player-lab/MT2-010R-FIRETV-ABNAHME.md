# MT2-010R – Fire TV Cube: reale Geräteabnahme / P0-Fixes

**Erfasst am 05.11.2026.** Quelle: Nutzerbericht nach Installation und Start der separaten MarsTube-0.0.10-Test-APK auf Amazon Fire TV Cube, Modell `AFTR`, ADB `192.168.41.24:5555`. **Beobachtungen sind real, Fehlerursachen zunächst Codeanalyse, noch keine getestete Korrektur.**

## Tatsächlicher Nutzerbefund

1. **P0 – BACK:** Die Zurück-Taste führt nicht verlässlich aus der Wiedergabe zurück. Erwartet ist ein klarer, nachvollziehbarer Rückweg aus Player/Info/Side-Rail zur Übersicht, bei gewolltem Zurückkehren zur Übersicht ohne unnötigen Stopp des Videos. Keine Dreifach- oder Zustandsabhängigkeit durch frühere Rail-Öffnung; keine Endlosschleifen.
2. **P1 – Menübreite:** Die Seitenleiste ist zu breit, nicht wie das freigegebene schlanke MarsTube-Design.
3. **P0 – Shorts-Navigation:** Ein Short startet; `D-PAD_DOWN` führt **nicht zum nächsten Short**, sondern kann ein normales breites Video starten. `D-PAD_UP` muss zum vorherigen Short im selben Shorts-Feed zurückführen. Buttons/Fokus dürfen diese Funktion nur übernehmen, wenn die entsprechenden Controls aktiv sind. Auch MEDIA_NEXT/PREVIOUS benötigen den passenden Queue-Kontext.

## Codebefunde (0.0.10, im GitHub-Branch vorhanden)

- `mt2-player-lab/MarsTvShell.java`: Rail im Player `dp(240)` (ca. Zeile 257), während `MarsTvRail.java` für die Hauptübersicht `dp(100)` setzt (ca. Zeile 154). Zwei inkompatible Breiten.
- `MarsTvShell.handleDown(KEYCODE_DPAD_UP/DOWN)` wechselt nur bei `isVerticalVideo()` den Clip. `isVerticalVideo()` liest `mPlayer.getVideoFormat()` und vergleicht `height > width`, statt anhand des **tatsächlichen Shorts-Feed-Kontexts/Video.isShorts/Queue** zu entscheiden. Beim frühen Videostart kann `getVideoFormat()` fehlen.
- `MarsTvShell.Actions.next()/previous()` werden in `patch_tv_shell.py` auf `mNextButton.performClick()` und `mPrevButton.performClick()` verdrahtet. Dies ist die **generische Next/Previous-Logik** und nicht als Shorts-only-Feed garantiert. Normales Video nach Pfeil-Down ist damit technisch plausibel.
- `MarsTvShell.handleBack()` / `handleDown(KEYCODE_BACK)`: `railEverOpened` bleibt gesetzt; wenn `focus == RAIL`, wird nur nach VIDEO zurückgesetzt, erst ein weiterer BACK kann die Aktivität minimieren. Verifiziertes Verhalten und Android-Back-Dispatch zuprüfen; keine willkürliche neue 3-Press-Logik. `patch_010_player_settings_ui.py` ergänzt `MobilePlaybackActivity.handleBack`, aber `MarsTvShell.handle(event)` wird davor von `dispatchKeyEvent` bedient. Beide Pfade müssen eine **gemeinsame**, nachvollziehbar getestete Zustandsmaschine verwenden.
- `mt2-player-lab/README-0.0.10.md` berichtet ausschließlich Unit-/CI-Tests vor Gerätetests. Deren PASS ist **kein** Nachweis, dass diese Funktionen auf dem Fire TV korrekt sind.

## Auftrag an OpenCode / Codex

- [ ] **Navigation vereinheitlichen:** Ein einziges dokumentiertes BACK-Verhalten für native KEYCODE_BACK (Down/Up), Android-Back-Callback und den physischen Fire-TV-Back-Button. Priorität: Dialog schließen → Rail/Player → Browse; Nutzer soll nicht in einer Schleife festhängen. Beim Rückweg zur Übersicht kann die Media3-Session im Miniplayer weiterlaufen, sofern aktiviert. Keine Fehlbedienung bei wiederholten BACK-Events.
- [ ] **Rail designgetreu:** Player-Rail optisch/maßlich mit dem schlanken Browse-Rail abgleichen; Fokus/Labels und Lesbarkeit erhalten; keine harte Breite 240 dp ohne begründetes UX-Ziel. Am besten eine gemeinsame Rail-Komponente/Dimensionsressource statt doppelter Komponenten.
- [ ] **Shorts-Queue modellieren:** Shorts-Wiedergabe anhand Video-/Feed-Kontext und eindeutiger Reihenfolge steuern, **nicht** allein nach Hochformat. Next/Prev müssen innerhalb des angezeigten Shorts-Feeds bleiben, ggf. weitere Shorts nachladen, und dürfen nicht in normale Related-Video-Empfehlungen wechseln. Kein Fallback zu normalem Video, wenn es keinen nächsten Short gibt: definierte Rückmeldung/Ende. D-Pad-UP/DOWN auch vor dem ersten decodierten Videoframe korrekt.
- [ ] **Tests:** Short A → Down → Short B → Up → A; 10× schnell wechseln; unbekannte `Format`-Metadaten; Video-Seitenverhältnis portrait, aber nicht Short; vorletzter/letzter Short; Fokus auf Controls; KEYCODE_MEDIA_NEXT/PREV; BACK bei offener Info, Rail und Player, mehrfach; Menübreite im Screenshot mit Referenz vergleichen.
- [ ] **Build:** Nur auf neuer `0.0.10R`-Testkennung oder kontrollierter Iteration der isolierten 0.0.10-Test-App aufbauen; 0.0.9 nicht löschen; Signatur, Version, Paketkennung, Prüfsumme, CI-Ergebnis und reale Testergebnisse dokumentieren. Bestehende reproduzierbare Patch-Pipeline nicht zerstören.
- [ ] **Dokumentation:** Diese Datei + `docs/PROJEKTSTATUS.md` um tatsächliche Änderungen, Logs und Geräteabnahme ergänzen. Offene Testfälle ausdrücklich als offen kennzeichnen.

**Keine Behauptung, dass die Korrektur bereits umgesetzt ist.** Dies ist die priorisierte Fehlerbeschreibung für den nächsten Entwicklungsdurchlauf.
