# MT2-037: Kontoprofile, Live/Musik, Shorts-Ablehnen & Langzeitliste

## Basis
- Baut A+B+C (0.0.36) unverändert in eine einzige MarsTube-App ein.
- Paket-ID `de.equiummc.marstube2`, Testversion 0.0.37 / Code 12037.
- Originalsignatur und aktualisierbare App-Daten bleiben erhalten; kein Uninstall.
- Fire TV Cube ist primäres Gerät.

## Konto- und Profilwechsel
- Der Profilbereich der Mediathek öffnet auf Android TV einen fokussierbaren
  Dialog statt eines Touch-Bottom-Sheets.
- Listet echte Identitäten aus `MediaServiceManager.loadAccounts()`, mit
  Profilbild, Kontoname, Mail, aktiver Markierung.
- Wechsel per `AccountSelectionPresenter.selectAccount(account)` an den
  bestehenden YouTube-Service; ohne Konto und Anmelden separat auswählbar.
- **Grenze:** 3 YouTube-Kanalprofile unter EINEM Google-Login erscheinen nur
  dann getrennt, wenn der vorhandene SignInService sie als Account-Identitäten
  liefert. Nicht behaupten, dass alle YouTube-Brand-Accounts automatisch da sind.
- Kein Entfernen von Konten, keine manuelle Tokenkopie oder Anmeldeumgehung.

## Navigation
- Live (Ziel 9) und Musik (Ziel 10) in der gemeinsamen Seitenleiste von
  Startseite, Player und Shorts. Icons aus vorhandenen Ressourcen.
- Route auf reale BrowsePresenter-Sections TYPE_LIVE und TYPE_MUSIC.
  Bei deaktivierter Rubrik klare deutsche Meldung statt leerer Dummy-Seite.
- Fokus und Zurück-Taste müssen auf Fire TV geprüft werden.

## Shorts
- „Nicht interessiert“ als **erste** Taste oben rechts beim Fokuswechsel.
- Bildschirmsymbol und OK verwenden denselben `performClick()`-Weg,
  ohne separate, möglicherweise falsch indizierte Aktionslogik.
- Weiter/Wiederholen folgt als zweites Symbol; Modussymbol aktualisiert
  ebenfalls Index 1. Likes/Kommentare bleiben den richtigen Symbolen zugeordnet.

## Speicher
- SQLite-Tabelle `hidden_video` mit `video_id` und monotoner Sequenz,
  eindeutiger Video-ID, maximal 50.000 Einträge.
- Erststart übernimmt vorhandene `mars_tv_interest_036.hidden_video_ids`
  einmalig und löscht die Quelldaten nicht.
- Nach 50.000 fällt die **älteste** abgelehnte ID heraus.
- Die lokale Sperrliste ist geräteweit, nicht automatisch identisch mit der
  YouTube-Server-„Nicht interessiert“-Liste; kontospezifische Sperren wären
  eine eigene Ausbaustufe.
- Ein `snapshot()` liefert die IDs an die Shorts-Discovery, die dann
  abgelehnte Videos nicht mehr vorschlagen soll.

## Live-Abnahme (nicht durch CI ersetzbar)
1. Mit drei auf YouTube sichtbaren Kanalprofilen prüfen, wie viele der
   SignInService tatsächlich als auswählbar meldet.
2. Konto auswählen -> Avatar/Name, Abos, Playlists und Startseite refreshen.
3. Auch „Ohne Konto“ und „Konto hinzufügen“ prüfen; keine Daten gelöscht.
4. Live aus Hauptmenu, normalem Player und Shorts-Player öffnen.
5. Musik analog, mit Ladezeiten und D-Pad-Fokus.
6. In Shorts rechts wechseln: X muss zuerst markiert sein und OK muss es
   sofort ausblenden. Nächstes Symbol ist Weiter/Wiederholen.
7. Zehn Ablehnungen, App-Neustart, abgelehnte Videos nicht erneut im Feed.
8. Migration der bisherigen 036-IDs ohne Rückkehr abgelehnter Videos.
9. Nach 50.000 IDs werden nur älteste verworfen; kein ungebremstes Wachstum.
10. A+B+C-Regressionscheck für Untermenüs, Discovery, Scrollen, Zurück-Taste
    und schnelle Media3-Wiedergabe.

## Status
- CI bestätigt ausschließlich Kompilierbarkeit, Testcode und signierten Build.
- Für Fire TV ist vor einer regulären Veröffentlichung eine manuelle Abnahme nötig.
