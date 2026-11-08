# MT2-036 – A+B+C: Gesamtpaket / Fire TV Cube

## Technischer Stand
- Entwicklungsbasis: 0.0.35 B (Build erfolgreich), darauf C für 0.0.36.
- Paket-ID `de.equiummc.marstube2`, `versionCode=12036`, dieselbe GitHub-Signatur.
- ZIP enthält genau eine APK, `SHA256SUMS`, `Install-MarsTube-036-FireTV.ps1`.
- **Nur ein Upgrade von der installierten 0.0.33**; keine Einzelinstallation von A/B.
- Build und Fire-TV-Livetest sind getrennte Abnahme-Gates.

## Punkt C – konkreter Umfang
1. **Shorts Discovery:** vorhandene `getShortsObserve()/continueGroupObserve()`-Pagination
   weiterverwendet. Neue Metadaten erst bei höchstens drei verbleibenden Shorts.
   Für nächste Videos *keine* Videostreams, Decoder oder neue PlayerViews vorladen.
   180 Videoobjekte im Short-Feed, bis zu 1000 tatsächlich angesehene IDs lokal.
   Sessionwechsel setzt Zähler, inFlight-Pagination und Callback sauber zurück.
   Doppelte IDs, angesehene und abgelehnte Videos werden herausgefiltert.
   Mehrfache END-Ereignisse führen nicht zu einer Aufruflawine (20 s Backoff).
2. **Weiter/Wiederholen:** Media3 REPEAT_OFF für Weiter, REPEAT_ONE für Wiederholen,
   Weiter am Listenende über dieselbe Shorts-Discovery; nie reguläre Vorschläge.
   Bestehende Pfeil-/Loop-Symbole im TV-Shorts-Panel werden übernommen.
3. **Nicht interessiert:** Short-Action löst direkt die lokale Ausblendung und
   den nächsten Short aus. Im normalen Player funktioniert der vorhandene
   Menüpunkt ebenso. Die Negativliste bleibt über Neustarts erhalten und ist
   auf 1.000 IDs begrenzt. Mit angemeldetem YouTube-Konto und vorhandenem
   Feedback-Token wird zusätzlich der originale Feedback-Dienst versucht.
   **Lokale Bestätigung ist niemals eine Garantie für erfolgte YouTube-Server-Rückmeldung.**
   Im Browser-Feed wird die Liste ebenfalls gefiltert. Nicht automatisch
   verwechseln mit Kanal-blockieren oder serverseitiger Profilbereinigung.
4. **Shorts Preview 9:16:** Die vorhandene Quelle `Video.cardImageUrl` hat kein
   garantiertes zusätzliches hochkantes Spezialbild. Bei echtem Hochkantbild
   direkte Vorschau, bei querformatigem JPEG keine Bildverzerrung, sondern
   `FIT_CENTER`, dann nur nach dem ersten verifizierten vertikalen Videoframe
   GPU-Vorhang (440 ms, Mitte nach außen), ohne Decoderwechsel/Videostartverzögerung.
   Kein Effekt bei regulärem Video, unbekannter Form oder Hochkantvorschau.
5. **Normale Video-Discovery:** Bestehende `BrowsePresenter.onScrollNearEnd`
   und `onScrollEnd` werden absichtlich beibehalten; keine neue
   Hintergrund-Autoplay-Queue für reguläre Videos in diesem Paket.
   Weitergehende normale Personal-Discovery ist ein separates späteres Feature.

## Fire-TV-Abnahme (vom Nutzer nach CI zu prüfen)
1. Nur mit OK auf einem fokussierten Short startet der Player.
2. Weiter: 20+ Shorts automatisch durchlaufen lassen, ohne Listenende bei
   verfügbarer Discovery. Auch bei nur 1-2 verbleibenden automatisch nachladen.
3. Wiederholen: nur aktuelles Short wiederholt sich; danach Weiter umstellen
   und mit Fernbedienung den Modus/Symbol kontrollieren.
4. Schneller Wechsel, 3x zurück; keine schwarzen Einzelbilder oder stale frames.
5. „Nicht interessiert“ aus Shorts und normalem Menü: sofort raus/weiter,
   nach App-Neustart keine erneute Empfehlung innerhalb der lokal gefilterten
   Listen. Ohne Anmeldung keine behauptete YouTube-Server-Bearbeitung.
6. Ein echtes 9:16-Thumbnail lädt direkt ohne Curtain; ein 16:9-Placeholder
   bleibt unverzerrt und öffnet sich beim ersten 9:16-Frame als kurzer Vorhang.
7. Normale Startseite, Abos, Mediathek, Uhrzeit/Datum und B-Shorts-Grid prüfen.
8. A-Player-Menüs (Qualität, Audio, Untertitel, Kommentar, Kanal, Glocke,
   Playlist) und Zurück-Taste auf Regression prüfen.
9. 10 Kaltstarts und 20+ Videowechsel mit TV-Fernbedienung.
10. Fire-TV-Tests bestehen: erst dann über Sony/Thomson sowie Veröffentlichung entscheiden.

## Verteilungssicherheit
PowerShell-7-Skript **nur** mit `-UpdateBestaetigt`; prüft ZIP-SHA256,
Paket-ID, `versionCode`, APK-Zertifikat, Fire-TV-Hardware, vorherige APK-Signatur
und keinen Downgrade. Installiert nur `adb install -r`. Abbruch bei
Zertifikatskonflikt, unlesbarer alter APK oder falschem Modell. Kein uninstall,
kein Datenreset, keine versteckte Live-Synchronisierung.
