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

## P0-Ergänzung: Miniplayer auf Fire TV nicht erreichbar

**Nutzerbefund vom 05.11.2026:** Nach BACK/Rückkehr in den Browse-Bereich läuft das Video teils als kleines Fenster **rechts über den Inhalten/Menübuttons** weiter. Dieses Fenster lässt sich mit der Fire-TV-Fernbedienung **nicht zuverlässig fokussieren und mit OK zum Vollbild vergrößern**. Nutzer muss blind an verdeckten Menüelementen navigieren. Gewünschtes Verhalten: Optional **weiterlaufender, bedienbarer Miniplayer beim Suchen/Browsen**, nicht pauschal abschaffen.

### Nachgewiesene Code-Stolpersteine

- `mt2-player-lab/patch_008_tv_player.py`: `marsTvMiniPlayerKey()` erreicht den Mini-Fokus über `KEYCODE_DPAD_UP` **nur** aus der ersten Videozeile und nur wenn `!mYouShowing` und `mMarsTvSelection` im ersten Rasterabschnitt liegt; `mMiniPlayerBar` muss sichtbar sein und `MiniPlayerBridge.isActive()`. Ein leicht erreichbarer Weg von beliebigen Browse-/Suchpositionen existiert damit nicht nachweislich.
- `mt2-player-lab/MarsTvRail.java`: Navigation zu Miniplayer (`destination == 7`) ist noch implementiert, der Menüeintrag wird in 0.0.10 jedoch **nicht** angelegt. Die Kommentierung erwähnt bewusst eine stets leere Mini-Zeile; das rechtfertigt nur, den Eintrag **ohne aktiven Miniplayer auszublenden**, nicht die Funktion bei aktiver Wiedergabe unzugänglich zu machen.
- `mt2-player-lab/MarsTvSettingsPages.java`: Es gibt bereits einen Miniplayer-Ein/Aus-Schalter. Die feste Overlay-Position und Fokuswege auf allen TV-Seiten sind bei der Geräteprüfung offen. Die Nutzerbeobachtung „überlagert rechts Menütasten“ muss per Screenshot/Focus-Trace bestätigt und dann behoben werden.
- Hier ist **der app-interne Miniplayer gemeint**, nicht zwangsläufig Android-System-Picture-in-Picture. Beide Funktionen nicht verwechseln.

### Verbindliches TV-UX-Ziel

1. **Sicher erreichbar:** Solange Mini-Playback aktiv und Miniplayer aktiviert ist, gibt es auf jeder Browse-/Suchseite einen eindeutigen Fernbedienungsweg zum Mini-Fenster, z. B. über einen **dynamischen „Laufendes Video“-/Miniplayer-Eintrag** in der linken TV-Rail; dieser Eintrag erscheint **nur bei aktiver Wiedergabe**, nicht als leere permanente Zeile. Alternativ ein ebenso zuverlässiger klar beschrifteter Shortcut; kein Zwang, zur ersten Kartenzeile zurückzuspringen.
2. **Sichtbarer Fokus:** Der aktive Fokus bekommt einen deutlich erkennbaren abgerundeten cyanfarbenen Rahmen. Mit **OK** öffnet sich unmittelbar derselbe Film wieder im Vollbildplayer, an der bisherigen Position und ohne unnötigen Neustart/Audio-Unterbrechung.
3. **Abdeckungsfrei:** Miniplayer in einer TV-sicheren Fläche positionieren; niemals wichtige Menüs, fokussierte Kacheln, Schaltflächen oder Suchergebnisse verdecken. Je nach Layout Inhalte sinnvoll einrücken/mit Abstand ausstatten, statt ein nicht fokussierbares Floating-Overlay über ihnen abzulegen.
4. **Navigation eindeutig:** D-Pad links/rechts/oben/unten, BACK, Wechsel zwischen Suche/Browse und Player stabil; wenn Mini fokussiert ist, führt BACK zum vorherigen Fokus zurück, ohne endloses Zurückspringen. Fokus nach Verkleinern an sinnvolle Position setzen.
5. **Bewusste Wahl:** Miniplayer per vorhandener Einstellung an/aus. Wenn deaktiviert, kein unfokussierbares schwebendes Videofenster und kein leerer Menüeintrag. Verhalten von Playback beim Zurückkehren (Stop oder Hintergrundaudio) explizit festlegen und prüfen, nicht stillschweigend ändern.
6. **Keine Doppel-Player:** Vorhandene Media3-Session weiterverwenden; keine neue Videoengine und möglichst keine zweite Decoderinstanz für die Vorschau starten.

### P0-Gerätetests und Akzeptanz

- [ ] Normalvideo starten → BACK zur Startseite → Miniplayer sichtbar → eindeutig fokussierbar → **ein OK** wieder Vollbild; gleicher Zeitstempel, Wiedergabe weiter.
- [ ] Suche öffnen, mehrere Ergebnisse durchblättern → Miniplayer erreichen, ohne blind zu navigieren; Suche und Navigation sichtbar und anklickbar.
- [ ] Nach Aktivierung/Deaktivierung Miniplayer: dynamische Navigation und Playback-/Overlay-Verhalten korrekt.
- [ ] D-Pad-Fokusrahmen auf Fire TV Cube und Thomson klar sichtbar; BACK-Pfad ohne Fokusfalle oder unerwünschtes Verlassen der App.
- [ ] Wechsel zwischen Browse, Suche, Profil, Abos und Verlauf, wenn Wiedergabe läuft; Player wieder öffnen, keine Mehrfachdecoder, kein Tonverlust.
- [ ] TV-Screenshots belegen, dass das Minifenster die Bedienung nicht überdeckt.

**P0: Zusammen mit BACK und Shorts vor weiteren kosmetischen Arbeiten beheben.** Dokumentierter Bug, **keine bereits umgesetzte Korrektur**.
