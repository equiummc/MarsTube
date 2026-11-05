# MarsTube 0.0.14 – Shorts-Maske, eine TV-Navigation und Updates sichtbar

## Konkreter Nutzertest (05.11.2026)
- Shorts sollen sich wie eine **eigene Maske** bedienen lassen, unabhängig vom normalen Video.
- D-Pad **Hoch/Runter** = vorheriger/nächster Short; **Links/Rechts** = 10 s Seek; **OK kurz** = Pause/Weiter; **OK halten** = vorübergehend 2× Geschwindigkeit. Fernbedienungs-Spezialtasten nicht voraussetzen.
- Linke Navigation in Home und Einstellungen optisch identisch. Im Player bei Bedarf einfliegen, in Einstellungen fest sichtbar; gleiche Breite, Icons und Fokusrahmen.
- Bisherigen Update-Eintrag hat der Nutzer in 0.0.12 nicht finden können. Update-Einstieg muss auf die erste TV-Einstellungsseite.

## Implementiert im Quellcode
- `MarsTvShell.java`: **explizite Shorts-Input-Priorität**: bei aktivem `MarsShortsSession` werden Hoch/Runter, Seek und Media-Nächster/Vorheriger vom Shorts-Zweig verarbeitet, ohne die Fokuszustände des normalen Players oder seine Empfehlungs-Kacheln aufzurufen. OK kurz pausiert, langes OK nutzt den bestehenden Media3-HoldSpeed(2f)-Pfad und stellt ihn beim Loslassen zurück. BACK und MENU behalten ihre explizite Navigation. Normales Playerpanel bleibt im Shorts-Modus verborgen.
- `MarsShortsSession.java`: Queue bleibt auch während schnellem Wechsel aktiv, wenn der Player noch den vorherigen Short meldet; normale Vorschläge verlassen die Queue nach explizitem Clear.
- `MarsTvNavigationTheme.java`: **einziger zentraler Satz** für 64-dp-Breite, 38-dp-Iconzellen, Farben, Hintergründe, Fokusrahmen und Navigationsicons. `MarsTvRail.java` und die Player-Rail in `MarsTvShell.java` verwenden diese gemeinsame Definition. Browse/Settings: feste Rail. Player: 150-ms-Einblendung per MENU, keine Dauerüberlagerung.
- `MarsTvSettingsPages.java`: „Auf Updates prüfen“ **direkt auf ROOT** unter „Updates & Info“ und weiterhin auf Info; echter `MobileUpdateActivity.startCheck()`-Aufruf, kein Dummy. Erklärung der neuen Shorts-Belegung unter Bedienung/Fernbedienung aktualisiert.
- `patch_012_tv_updates.py` akzeptiert nun die im Repository direkt hinterlegte, korrekte Update-Aktion, statt sie beim Bauen erneut erzwingen zu wollen.
- Reproduzierbarer `patch_014_tv_shorts_mode.py` nach 013 im bestehenden Build; **eigene** Testkennung `de.equiummc.marstube2.firetest014.debug`, `versionCode 12014`, `versionName 0.0.14`. Die installierten 0.0.12/0.0.13 bleiben unangetastet.
- Erweiterte Shorts-Session-Regression: schnelle Umschaltung während `prepare` bleibt im Shorts-Modus.

## Reale Fire-TV-Abnahme (Nutzerrückmeldung 05.11.2026)

- [x] Shorts-Seite öffnen und eine Videokachel per Fernbedienung auswählen
- [x] Während Shorts: Hoch/Runter wechseln; Links/Rechts spulen; OK kurz pausiert; OK gehalten schaltet vorübergehend 2×
- [x] Startseite, Einstellungen und Player mit derselben schmalen linken Navigation
- [x] „Auf Updates prüfen“ direkt auf der ersten Einstellungsseite sichtbar
- [ ] **BACK im Video:** Rückkehr zur vorherigen Übersicht mit einem Druck ist nicht als erfolgreich bestätigt. Diagnoseschritt: echte Fernbedienung und gezielt `adb shell input keyevent 4` vergleichen, Vordergrund-Activity vor/nach protokollieren. Nur den BACK-/Miniplayer-Handoff ändern, nicht die vier bestätigten Bereiche.

## Noch offen
- [ ] CI Android-Komplettbuild / Units von 0.0.14 erfolgreich.
- [ ] Reales Fire-TV-Gerät: Kurz/Long-OK, Seek, Shortswechsel, MENU, BACK, Home/Settings-Fokus, Designvergleich.
- [ ] Video in Shorts-Maske korrekt zentriert im 9:16-Verhältnis, ohne unerwünschte Verformung oder überlappende Navigationsflächen.
- [ ] Einstellungsseite: direkter „Auf Updates prüfen“-Eintrag *sichtbar* und per Fire-TV-Fernbedienung auswählbar; externe Update-Quelle noch möglicherweise nicht veröffentlicht.
- [ ] Echte 0.0.12→0.0.13 oder 0.0.13→0.0.14 In-App-Aktualisierung: bestehende APKs haben unterschiedliche Test-Paketkennungen/CI-Signaturen. Ohne kontrollierte stabile Signatur und passende Release-APK ist ein direktes Update **noch nicht** bestätigt.

**Freigaberegel:** Ein grüner Gradle-Build beweist nur Kompilierung/automatisierte Tests, nicht die echte D-Pad-Erfahrung. Keine Gerätefreigabe ohne Fire-TV-Abnahme.
