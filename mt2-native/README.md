# MarsTube 2.0: nativer TV-Unterbau (nicht der NewTube-Mobile-Prototyp)

## Verbindliche Codebasis

**Eigentümer der App und ihrer Oberfläche ist der MarsTube-TV-Code von `mars/stable`.**
Der Zweig `feature/mt2-native-marstube-shell-20261104` wurde von diesem Stand
abgezweigt, NICHT vom NewTube-Smartphone-Experiment.

Bestehende native Android-TV-Komponenten:

- `smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/browse/BrowseFragment.java`:
  Leanback-Seitenmenü und TV-Remote-Fokus.
- `.../tv/ui/playback/PlaybackFragment.java`: TV-Wiedergabeoberfläche.
- `common/src/main/res/values-de/strings.xml`: bestehende echte deutsche UI-Texte.
- `smarttubetv/src/main/res/values/strings.xml`: Produktname `MarsTube`.

Die `mt2-player-lab`-Dateien im bisherigen Experiment sind **ausschließlich technische
Quellen / Vergleichsaufbau für Media3-SABR**. Ihre `stmobile`-Activities, Settings,
App-Info, Update-Dialoge, Branding und mobile Navigation werden NICHT in die native
MarsTube-App übernommen. Neue installierbare Versionen dürfen nicht aus dem
NewTube-Mobile-Flavour als fertige MarsTube-Version bezeichnet werden.

## Ausdrücklicher UI-Vertrag

1. **Start und Navigation:** einzig das echte, schmale MarsTube-TV-Menü. Es wird mit
   D-Pad LINKS vom Inhaltsbereich aufgerufen und ist mit HOCH/RUNTER/OK bedienbar.
   D-Pad RECHTS bringt den Fokus in die Videoliste zurück. Kein Touch-Fallback.
2. **Menüstruktur:** Start, Suche, Abos, Verlauf, Bibliothek, Einstellungen, Über MarsTube.
   Kein Nutzer-Endpunkt verweist auf eine fremde App-Einstellungsseite.
3. **Einstellungen:** deutsche, inhaltlich verständliche Begriffe. Jede Ebene ist mit
   ZURÜCK zur vorherigen Ebene erreichbar; kein Activity-Backstack-Trick, der den
   laufenden Player oder den gesamten Task unerwartet beendet.
4. **Player:** Videobild hat Vorrang. Bei ausgeblendeten Controls weder Uhr noch
   M-Logo oder Werbung/Brand-Wasserzeichen. D-Pad RUNTER öffnet eine flache
   6er-Bildleiste (16:9, min. zwei Titelzeilen); HOCH die kleinen Player-Tasten.
   Unveränderte Media3-Decoder/Streaming-Pipeline aus dem nachgewiesenen Fire-TV-Test
   erst an einer **klar definierten Player-Schnittstelle** integrieren.
5. **Branding:** sichtbarer Anwendungsname MarsTube, echtes blaues M als Launcher-
   Icon / im Hauptmenü. Keine Logos fremder Apps, keine irreführende Versionsanzeige.
6. **Über / Updates:** zeigt nur MarsTube-Version und MarsTube-Quellen. Kein
   automatischer Updater zu NewTube/SmartTube oder fremden Paketsignaturen. Eigene
   Updateversorgung erst nach signierter Release-Pipeline anbinden.
7. **Dienstbezeichnungen, Lizenzen:** Der genutzte Videodienst darf in
   sachlich notwendigen Hinweisen als YouTube bezeichnet werden; solche Fakten,
   Copyright-Hinweise und Open-Source-Lizenzen werden nicht verfälscht oder gelöscht.
   Das PRODUKT heißt jedoch ausschließlich MarsTube.

## Abnahme / Reihenfolge

- [x] Von `mars/stable` abgezweigt; kein weiterer Mobile-App-Rebrand.
- [x] Originales M-Symbol als native Launcher-Ressourcen hinterlegt, ohne
  Produktnamen im Icon.
- [x] Quellenprüfung für MarsTube-Label, Launcher-Icons und deutsches TV-Menü.
- [ ] Native TV-Navigation auf Fire TV Cube: alle Seiten, Einstellungen,
  zwei Untermenü-Ebenen, jeweils ZURÜCK, keine Sackgasse.
- [ ] Neue TV-Player-Controls + 6er-Karussell in nativer `PlaybackFragment`.
- [ ] Bewährten Media3-SABR/DASH-Player über ein eigenständiges Adaptermodul portieren
  und Audio / Bild / Startzeiten auf AFTR erneut testen.
- [ ] Unter `Über MarsTube` korrekter Versions- und Update-Endpunkt, keine Upstream-
  Installationsfunktion.
- [ ] Erst danach eigenständige Test-APK bereitstellen; niemals eine ungeprüfte
  native UI mit altem Player als fertige Media3-Version ausgeben.

**Wichtig:** Dieser Commit legt die korrekte native Quelle und die Branding-
Grundlage fest. Er ist NOCH KEIN Release und enthält NOCH KEINEN Media3-Port
in die ursprüngliche MarsTube-App. Die stabilen Geräteinstallationen bleiben
bis zur Hardwareabnahme unverändert.
