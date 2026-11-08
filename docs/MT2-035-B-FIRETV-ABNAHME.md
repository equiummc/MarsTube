# MT2-035 Punkt B – TV-Übersichtsseiten (kumuliert mit A)

## Grundlage und Verteilung
- Branch: `feature/mt2-035-overview-b-20261108`, aus erfolgreicher MT2-034 A.
- Dieselbe App `de.equiummc.marstube2`, Build-Version 0.0.35 / Code 12035.
- Kein separates Nutzer-Update: 0.0.35 ist ein Zwischenstand für den nächsten gemeinsamen Stand nach C.
- Nur Android TV wird angepasst; Handy- und Basis-Media3-Logik bleiben unverändert.

## Umgesetzt im Quellcode
- **Startseite:** Die vorhandene Nutzerwahl für 4/6/9 Kacheln bleibt gültig;
  kompaktere Innenabstände und kleinerer TV-Text schaffen mehr Freiraum.
  Keine Änderung des Video-Backends.
- **Shorts-Übersicht:** Drei 9:16-Karten je Reihe; vertikale Bildhöhe auf zwei
  sichtbare Zeilen ausgelegt (6 gleichzeitig als Abnahmeziel). Keine verzerrten
  Thumbnails und kein Autoplay durch Fokus.
- **Uhrzeit und Datum:** Oben rechts auf den Browse-Seiten, deutsche lokale Systemzeit;
  Cast- und Suchaktionen bleiben links davon erreichbar. Im Shorts-Player während des
  geöffneten Navigationsmenüs, regulärer Player aus A/033 unverändert.
- **Abos:** Links wählbare Kanalnamen aus *bereits geladenen* Videos des echten
  Abonnements-Feeds; rechts bleibt das echte Video-RecyclerView mit Videostart,
  Pagination und Vorschaubildern. Die Aboliste ist ausdrücklich kein behaupteter
  vollständiger Account-Export, solange nicht sämtliche Abo-Kanäle geladen wurden.
  Alle/Kanalfilter, Fokuswechsel links/rechts und Scrollnavigation.
- **Mediathek:** Schnellzugriffe Verlauf, Später ansehen, Playlists, Musik oben im
  You-/Profilbereich, einschließlich Fokus. Verweist auf echte vorhandene Sektionen.
  Falls Später ansehen nur als Playlist und nicht als eigene Sektion verfügbar ist,
  führt der Shortcut zu Playlists mit Hinweis statt erfundener API-Navigation.
- **Keine** zweite App, kein Datenreset, keine unnötige Video-Vorladung.

## Verbindliche Fire-TV-Abnahme für B
1. Auf der Startseite je nach gespeicherter Auswahl 4/6/9 leicht verkleinerte
   Kacheln; Fokus alle Spalten mit Pfeiltasten und OK erreichbar, Titel lesbar.
2. Startseiten-Uhrzeit + Datum rechts oben, lokale Zeit korrekt, Suche/Cast erreichbar.
3. Shorts-Übersicht 3 × 2 Hochkant-Kacheln, korrekte 9:16-Proportionen, ohne Autostart
   beim Fokus; Zurück und Auswahl erhalten.
4. Abo-Seite: linke Liste „Alle“ + sichtbare Kanäle, rechter Video-Feed, Trennbereich.
   Links/Rechts-Fokus; Filtern zeigt nur geladene Kanalvideos.
5. Abo-Feed aktualisiert: keine doppelten Kanäle, kein Bild-/Fokusflackern;
   fehlende Kontoanmeldung zeigt die bisherige Anmeldung statt leerer Fake-Ansicht.
6. Mediathek: vier sichtbare Schnellzugriffe und normale bisherigen Rows darunter,
   Verlauf/Playlists/Musik führen zur echten Sektion. Später ansehen ist nur direkt,
   wenn eine echte Sektion existiert, sonst transparenter Playlist-Fallback.
7. Beim Schließen der Mediathek bleibt die Abo-Liste nicht sichtbar.
8. Clock im Shorts-Player bei geöffneter Navigation, nicht ständig über Videobild.
9. Nach 20 Seiten-/Videowechseln kein schwarzer Frame, kein TV-Back-Regression.
10. Thomson-Kompaktmodus und Sony erst nach Fire-TV-Freigabe kontrollieren.

## Vorbehalte
Ein grüner Build beweist Kompilierbarkeit/automatisierte Tests, **nicht** die
Pixelwirkung oder echte Fernbedienungsbedienung. Keine Freigabe ohne TV-Bilder.
Nach B folgt C; erst dann gemeinsam signiertes Paket zum Nutzertest.
