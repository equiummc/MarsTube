# MT2-017 – Shorts Split-Screen auf Android TV

**Stand:** 06.11.2026, **Entwicklungszweig:** `feature/mt2-017-shorts-stage-20261106`.
**Basis:** `feature/mt2-017-stream-recovery-20261106`, vorheriger Produktstand `0.0.16`.
**Ziel:** Version `0.0.17`, ausschließlich Paket `de.equiummc.marstube2`.
**Freigabe:** NOCH NICHT erteilt; CI/TV-Tests müssen erfolgreich sein.

## Nutzeranforderung

Die vorherige Drei-Kachel-Shorts-Übersicht und separate Vollbildseite werden auf dem TV durch eine durchgehende Shorts-Seite ersetzt: rechts sofort das erste 9:16-Video, links vier nächste Vorschauen unter einer groß lesbaren Uhrzeit. Das Glas-/Blau-Design der aktuellen MarsTube-Oberfläche beibehalten. Die Vorschau rückt im Feed weiter, ein sanfter Zweier-Rhythmus vermeidet übermäßige Übergangsanimation. Der Player behält einen 2–3 px schmalen blau/cyanfarbenen Fortschrittsbalken und links/rechts aktuelle Zeit/Gesamtdauer.

Der Schalter „Automatisch weiter“ wird sichtbar, standardmäßig EIN, persistent; zusätzlich als per Fernbedienung erreichbarer Punkt „Shorts Auto“ in der eingeblendeten Rail. Videoende: nur zum nächsten Eintrag der *echten Shorts-Queue* wechseln, nicht zum normalen Video. Bei AUS oder Feed-Ende anhalten. D-Pad: Hoch/Runter vorheriger/nächster Short; Links/Rechts 10 s und mit Halten beschleunigtes Scrubbing; einmaliger Media3-Seek beim Loslassen. OK kurz Pause/Weiter, OK lang 2× wie bisher, BACK öffnet bestehende Rail. Der Shorts-Ladekreis erscheint erst nach fünf Sekunden, sofern dann noch gepuffert wird.

Home-, Shorts-, Suche-, Abo-, Verlauf-, Einstellungen- und Profil-Symbole sind einheitliche 24-dp Android-VectorDrawable-Konturicons. Keine zusätzliche Playerinstanz, kein echter GPU-Blur, keine Änderung am SABR-/Codec-/Media3-Transport.

## Implementierungsstand im Featurebranch

- `mt2-player-lab/MarsTvShortsStage.java`: leichte TV-Split-Screen-Oberfläche inkl. Uhr, vier nächsten Videos, Laufzeit, Fortschritt, Auto-Schalter und Animation
- `mt2-player-lab/MarsShortsSession.java`: Queue-Funktionen `currentIndex`, `upcoming`, `hasNext`
- `mt2-player-lab/MarsTvShell.java`: bestehenden Media3-View umpositionieren; neue Shorts-Seite; Videoende Auto-Weiter; D-Pad-Hold-Seek; bestehende BACK-/OK-Logik erhalten
- `mt2-player-lab/MarsTvNavigationTheme.java` und `assets/drawable/mars_tv_ic_nav_*.xml`: Vektoricons
- `mt2-player-lab/patch_017_shorts_stage.py`: reproduzierbare Overlay-Patches für TV-Player, Start in Shorts auf derselben Seite, 5-s-Spinner
- `mt2-player-lab/MarsShortsSessionTest.java`: Queue-/Vorschau-/End-of-feed-Tests erweitert
- `mt2-player-lab/production-version.json`: 0.0.17 / 12017 / permanente App-ID
- `tools/build-marstube-production.sh`: Overlay-Reihenfolge mit 017 ergänzt; bestehende Release-Signierungspflicht bleibt bestehen
- `.github/workflows/mt2-017-shorts-ci.yml`: eigener Compile-/Unit-Testlauf ohne Veröffentlichung oder produktive Signierung

## Prüfungen vor Installation / Release (offen)

1. GitHub CI: feste NewTube-Quelle, alle Overlays, MarsTvShortsStage Java/Kotlin Compile, `testStmobileDebugUnitTest`, `assembleStmobileDebug` ohne Fehler. Fehler- und Resultatlink nachtragen.
2. Die bestehende APK 0.0.16 darf nicht verändert werden. Neues Release muss `0.0.17`, `12017`, `de.equiummc.marstube2` und **denselben Besitzer-Signaturschlüssel** verwenden. Release-Feed, SHA-256 und Paket-Updater testen.
3. Realer Fire TV Cube: Shorts-Start ohne zweites OK, 9:16 rechts vollständig sichtbar, vier Vorschauen lesbar; Uhr, Farbverlauf, Fortschrittsbalken und Spulzeit stimmen; BACK öffnet Overlay ohne Unterbrechung.
4. D-Pad bei normalen und Shorts-Videos: zehn Sekunden beim Tippen, fortlaufendes zunehmend schnelleres Scrubbing bei Halten, ein Seek beim Loslassen, kein schwarzer Screen oder Hänger.
5. Auto-Auswahl EIN/AUS bleibt nach Neustart erhalten; am letzten Short stoppen, nie ein normales Video als automatisches nächstes. Puffer-/Netzfehler dürfen keinen Endlos-Autowechsel auslösen.
6. Erst danach Thomson Android 14: wenig RAM, Startzeit, Scrollen, Fokus und Glide-Thumbnail-Speicher; ggfs. begrenztes Nachladen.
7. Ladeanimation: bei 1–4 Sekunden keine Anzeige, nach 5 Sekunden beim echten Buffering sichtbar; keine dauerhaft hängende Spinner-Callback.

**Kein Abschlussnachweis:** Bis tatsächliche CI-/Geräteergebnisse erfasst sind, sind „Build erfolgreich“, „APK installierbar“ und „Produktivrelease fertig“ ausdrücklich nicht bestätigt.
