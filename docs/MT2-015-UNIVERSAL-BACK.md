# MT2-015 – BACK bringt immer zur Navigation

**Auslöser:** Fire-TV-Nutzertest von 0.0.14 am 05.11.2026. Shorts-Shortcuts, Auswahl, UI-Farbgebung und Update-Eintrag bestätigt. Nicht abgenommen: BACK/Focus, besonders nach Scrollen in einem weit rechten Shorts-/Home-Kartenbereich und von Einstellungs-Unterseiten.

## Konkrete Abnahmevorgaben
- BACK **aus jeder TV-Browse-Sektion** öffnet sofort die linke Rail, unabhängig von aktuellem Grid-Index; die Kategorie bleibt erhalten, kein Rücksprung durch 20+ Kacheln.
- BACK aus **Settings und Unterseiten** fokussiert dieselbe Rail, ohne vorherigen zwanghaften Wechsel nach Home. Erneutes BACK lässt die Rail fokussiert. D-Pad RECHTS aus der Rail fokussiert sofort die erste sichtbare, wählbare Einstellungszeile. Kein „ganz runter und wieder hoch“.
- BACK während **normalem Video oder Short** öffnet die Rail als Overlay, das Video wird weder minimiert noch gestoppt. MENU bleibt alternative Rail-Taste. Rechts aus der Player-Rail kehrt zu den Playerbedienelementen zurück; bei Shorts direkt ins Short-Video.
- **Home, Suche, Player**: schmale 64-dp-Icon-Rail. **Einstellungen**: 132-dp-Fixleiste mit denselben Icons, Farben und Fokusrahmen plus Beschriftung **unter** den Icons.
- Die Suchlupe und andere Nav-Symbole werden in allen Rails auf **20 dp** begrenzt; keine intrinsischen Drawable-Größen, die überlappen.
- Im Player verschwindet die nicht aktive Rail; das Player-Panel reserviert nicht dauerhaft den Platz für eine versteckte Rail.
- Bestehende, bereits erfolgreiche v0.0.14-Shorts-Belegung, Update-Einstieg und Media3-Player **unverändert**.

## Code
- `MarsTvRail.java`: BACK/MENU vor jeder aktuellen-Fokus-Auswertung, überall derselbe Fokuspfad. Settings-Rail 132 dp, Labels unter gebundenen 20dp-Icons. `focusSettingsContent()` sucht statt rekursivem `focusSearch()` die tatsächliche, sichtbare Settings-RecyclerView und die erste auswählbare Zeile; bounded Retry für Layout-Rennen.
- `MarsTvNavigationTheme.java`: identische Layoutparameter, zusätzliche `SETTINGS_WIDTH_DP = 132`.
- `MarsTvShell.java`: BACK → `openRail()` (kein `minimize()`), UI bleibt während Wiedergabe aktiv; begrenzte 20dp-Icons, wenig Blindabstand links, keine abgeschnittenen Labels in der 64dp-Rail.
- `patch_015_universal_back.py`: aktualisiert nur das temporäre Testpaket `de.equiummc.marstube2.firetest015.debug` / 12015. Fügt **nur auf Seiten mit installierter TV-Rail** in `MobileActivity.onPostResume` einen AndroidX `OnBackPressedCallback` hinzu, damit BACK auch dann ankommt, wenn die Plattform keinen KeyEvent liefert; die Playback-Activity behält den eigenen Callback. Entfernt außerdem das frühere BACK-Abfangen durch den Miniplayer und den Home-Rücksprung aus dem ursprünglichen Browse-Keyhandler.

## Realtests auf Fire TV Cube (noch offen)
- [ ] Browse weit nach rechts scrollen → BACK → Rail fokussiert und Scrollposition erhalten.
- [ ] Shorts öffnen und mindestens 20 Karten nach rechts navigieren → BACK → Rail fokussiert und Kategorie Shorts bleibt.
- [ ] Einstellungen öffnen → BACK → Rail fokussiert → RECHTS → wählbare Einstellungszeile sofort fokussiert. Auf Unterseiten wiederholen.
- [ ] Video/Short abspielen → BACK → 64dp-Menü fliegt links herein, Ton/Bild laufen weiter, kein Miniplayer-Zwang.
- [ ] Von Player-Rail über Einstellungen navigieren, dann BACK/RECHTS; keine Navigation-Sackgasse.
- [ ] Suchsymbol vollständig innerhalb der Rail; keine Überlappung mit fremden Seitenelementen.
- [ ] Vier bestätigte v0.0.14-Tests erneut kurz als Regression prüfen.
- [ ] Langfristiger In-App-Update-Pfad bleibt gesondert: verschiedene kurzlebige CI-Signaturen verhindern ein direktes Upgrade von `firetest014`.

**Build-Erfolg ist kein Geräte-Abnahmetest.** Testpaket 0.0.15 installiert parallel zu 0.0.14; keine Alt-APK wird deinstalliert.
