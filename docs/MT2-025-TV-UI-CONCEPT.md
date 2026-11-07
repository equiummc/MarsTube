# MT2-025 – TV-UI-Konzept / kontrollierte Umsetzung

Stand: 07.11.2026. Ausgangsrelease: v0.0.24. Ziel: v0.0.25, permanente App-ID `de.equiummc.marstube2`.

## Original-M als feste Markenidentität

Das einfarbige weiße Mars-M wird bytegleich aus `mt2-player-lab/assets/drawable/mars_m_flat.xml` übernommen. Diese Vektorform stammt aus dem alten Mars-TV-Projekt; weder Inhalt noch Silhouette werden verändert. Bei Fokus wird der bestehende Vektor hellblau eingefärbt (Tint), nicht neu gezeichnet.

## Navigationsvertrag

Ein gemeinsames `MarsTvNavigationModel`, `MarsTvNavigationTheme` und `MarsTvRailLabel`; zwei View-Adapter für normale Activities und die Player-Overlay-Fläche, weil Player-Eingaben ohne Unterbrechen der Wiedergabe verarbeitet werden müssen. Das ist **eine zentrale Menüdefinition und Designlogik**, aber technisch nicht eine einzige View-Instanz über alle Android-Activities.

7 dauerhaft sichtbare Ziele in identischer Reihenfolge:

1. Suche
2. Abos
3. Verlauf
4. **M / Startseite** (Index 3)
5. Shorts
6. Profil
7. Einstellungen

BACK im Player / Menüaufruf: M als Fokus-Anker; zyklisches Auf/Ab bleibt. In Einstellungen bleibt BACK für die Fragment-Hierarchie erhalten. Schmale, mittig schwebende blaue Glas-Kapsel ohne GPU-Blur. Neues gleich skaliertes 24dp Outline-Symbolset aus Android-Vektoren. Das Tooltip rechts wird ausschließlich per Translation positioniert, um den doppelten Y-Offset aus 024 zu beheben.

## Shorts-Layout

- **Eine** bestehende Media3-PlayerView, keine zweite Dekoderinstanz.
- Mittig positionierte feste **9:16-Hochkantfläche**, 22dp oben und mehr als 20dp unten frei, mit Rundungs-Outline.
- Links **exakt zwei** 9:16-Vorschaubilder, echte Queue und Glide-Bildcache, in abgerundeten Karten.
- Rechts kleine schwebende Leiste: Uhr, Like, Dislike, Spielzeit und Dauer.
- Dünne Fortschrittsleiste unmittelbar unter dem Hochkantvideo.
- Keine Kommentare, kein Teilen, kein erzwungener Vorabladeprozess.
- Speichern, separate Next- und Seek-Buttons werden nicht in der Aktionsleiste präsentiert; Spulen läuft über die fokussierbare Fortschrittsleiste.
- Bestehende Auto-Weiterschaltung bleibt erhalten und ist in Einstellungen → Darstellung → Shorts über einen echten Schalter erreichbar.
- Kein Stretching: Media3 `RESIZE_MODE_FIT`, ein Portrait-Video bleibt vollständig sichtbar. Android-TV SurfaceView-Clipping und 9:16-Sichttest auf Fire TV müssen praktisch überprüft werden.

## Sonstige Hauptbereiche

Suche, Startseite, Abos, Profil, Verlauf, alle Einstellungsseiten und der Videoplayer nutzen die identische globale Navigation. Der interne Inhalt bleibt zunächst in den existierenden reellen Seiten, damit keine vorgetäuschten Suchtreffer, Konten, Abos oder History-Daten entstehen.

Ein zusätzlicher Datumsfilter für Verlauf (Gestern/Woche) ist fachlich als mögliche Erweiterung vorgemerkt. **Nicht als implementiert ausgeben**, solange kein zuverlässiger Zeitstempel der angesehenen Videos und ein verifizierter Filter vorliegt.

## Test- und Release-Gates

- [ ] Gesamte gepinnte Patchkette 001–025 durchlaufen
- [ ] Java-Unit-Tests und kompilierte Debug-APK
- [ ] Stabile Updatequellen/versionscodes, Original-M-Bytevergleich, kein geänderter Player-BACK-Handler
- [ ] Signierter Produktions-Build auf GitHub, APK-Certs und Prüfsummen
- [ ] Fire TV: Mainrail, alle Menüs, Shorts mittig, Rundung, zwei Preview-Karten, rechte Buttons, 9:16 Fit, Miniplayer-Fokus
- [ ] Thomson: Performance/Fokus, keine zusätzliche Hardwarelast

Code-Build bestätigt nur Kompilierung; Gerätebefund bleibt offen. Der produktive GitHub-Release ist die einzige Updatequelle und wird erst nach erfolgreich signiertem Build veröffentlicht.
