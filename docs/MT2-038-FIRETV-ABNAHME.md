# MT2-038 – YouTube-Profile, Live & Musik | Fire TV Cube

## Cumulative update
- Branch: `feature/mt2-038-profiles-live-music-20261109` auf erfolgreichem 0.0.37.
- APK `de.equiummc.marstube2`, `versionCode=12038`, dieselbe dauerhafte Signatur, kein zweites Paket.
- A+B+C und profilgetrennte SQLite-Negativliste (100.000 pro Profil) bleiben enthalten.
- Kein Einzel-Update von 0.0.37 nötig, kein automatischer Download oder Gerätezugriff.

## Profile
- Mediathek/Kontozeile und separate TV-Zeile „YouTube-Profil wechseln“ öffnen einen
  fernbedienungstauglichen Dialog; echte Konten werden nur über
  `MediaServiceManager.loadAccounts` abgerufen.
- Name, optionales Profilbild, Konto-E-Mail und Aktiv-Markierung im Dialog.
  Mit OK über `AccountSelectionPresenter.selectAccount` wechseln, keine eigene
  Authentifizierung, kein automatisches Abmelden; „Ohne Konto“ behält Tokens,
  „Konto hinzufügen“ startet die vorhandene YouTube-Geräteanmeldung.
- Nach Wechsel alter lokaler FeedCache und Shorts-Sitzung verwerfen, Startseite
  über den vorhandenen BrowsePresenter neu laden. Die 100.000er-Rejection-Liste
  folgt der **wirklich aktiven** Konto-/Kanalidentität.
- **Grenze:** Es werden exakt die Konten/Kanäle angeboten, die der verbundene
  YouTube-Dienst liefert. Ob alle drei bekannten YouTube-TV-Profile geliefert
  werden, muss mit einer echten Anmeldung auf dem Fire TV überprüft werden.
  Keine erfundenen Profile oder private Account-IDs im Log.

## Live und Musik
- Neun gemeinsame Navigationseinträge in `MarsTvNavigationModel`,
  unveränderter M-Home-Index 3, gleiche Symbole/IDs auf Browse, Shorts und Player.
- Live-ID 9 führt auf `MediaGroup.TYPE_LIVE`, echten
  `BrowsePresenter.getContentService().getLiveObserve()`-Feed.
- Musik-ID 10 führt auf `MediaGroup.TYPE_MUSIC`, echten
  `getMusicObserve()`-Feed. Keine fest angelegten Demo-Kacheln.
- Ressourcen: `mars_nav_live_038.xml` und `mars_nav_music_038.xml`.
  Bei Navigation aus einem Player wird nur die Browse-Activity mit Ziel-ID
  geöffnet; es gibt keinen zweiten Decoder und keine Playback-Migration.
- Normale Video-Kacheln haben weiterhin direktes X; die rechte Shorts-Leiste
  zeigt das X als erste Aktion.

## Abnahme
1. In Mediathek Kontozeile/„YouTube-Profil wechseln“ per D-Pad/OK öffnen.
2. Profile anzeigen, erstes und drittes Profil auswählen, aktiven Haken prüfen.
   Prüfen, ob alle drei erwarteten YouTube-Profile vom Konto-Dienst geliefert werden.
3. Nach Profilwechsel Startseite/Abos/YouTube-Personalisierung aktualisiert,
   keine gemischten Cached Cards; verschiedene abgelehnte IDs bleiben getrennt.
4. „Ohne Konto“ wählen, eigene lokale Liste prüfen; gespeicherte Anmeldungen
   bleiben für Wechsel erhalten.
5. Live links im normalen Browse und im Player aufrufen: echte Livestream-Tiles
   laden, Fokus und Wiedergabe funktionieren.
6. Musik links im normalen Browse/Player aufrufen: echter Musikfeed;
   Videoqualität und Autoplay bleiben gleich.
7. Normale Video-X, Shorts-X, 100.000er SQLite-Grenze und Migration 036->037
   erneut auf Regression prüfen.
8. Back/Menü, D-Pad zwischen Player und Rail sowie die neun Einträge testen.
9. Fire TV Cube zuerst; Thomson/Sony später. Kein Release vor Geräteabnahme.

## CI vs. Fire TV
Gradle- und Unit-Tests verifizieren technische Integrität; die native
YouTube-Anmeldung und D-Pad-Fokus benötigen reale Tests mit dem Fire TV Cube.
Installation darf ausschließlich mit geprüftem Zertifikat und `adb install -r`
über die vorhandene App erfolgen.
