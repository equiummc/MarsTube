# MarsTube – verbindliche visuelle Referenz für Player 0.0.11

**Nutzerfreigabe:** 04.11.2026. Dieses Dokument beschreibt **das ausdrücklich freigegebene 16:9-Screenshot-Zielbild** `neon_blaues_marstube_video_interface.png` (Originalgröße 1672 × 941 Pixel). Es ist **nicht** die Beschreibung eines neuen Designvorschlags. Die Original-PNG existiert im Chat-Artefaktbestand, muss aber noch manuell ins Repository unter `design/reference/marstube-ui-target.png` eingecheckt werden. Bis dahin **kein anderes Bild als verbindliche Vorlage ausgeben**.

## Bildaufbau der freigegebenen Vorlage

- Dunkelblau/schwarze Vollbildbühne mit feinen cyanfarbenen Lichtakzenten; keine großflächig milchigen Flächen und keine dauerhaft animierte Blur-Schicht.
- **Linke schmale Navigationsleiste**: ca. 8 % Bildbreite, M-Logo oben, darunter senkrechte quadratisch-abgerundete Icon-Felder. Aktiv ist ein auffälliger cyanfarbener Rahmen. Keine breite Text-/Kachelspalte.
- Zentriertes vertikales Short-Video (9:16) auf der Bühne, links und rechts schwarze/nebeldunkle Balken. Video niemals horizontal auf 16:9 strecken.
- **Große abgerundete Playerleiste** von knapp rechts der Navigation bis zum rechten Bühnenrand. Blau-transparent mit feinem Cyan-Konturrand, nach unten dunklerer Farbverlauf; keine schweren Live-Blur-Effekte.
- **Fortschritt oben in der Playerleiste**: Zeitanzeige links, kräftiger cyanfarbener Track mit weißem Schieber, Zeitmarkierung rechts.
- **Bedienbuttons in der nächsten Reihe horizontal zentriert**. Hochwertige klare weiße Piktogramme in einzeln abgerundeten dunkel-cyanblauen/glasartigen Schaltflächen; ausgewählte Pause/Play-Schaltfläche besonders betont mit hellerem cyanfarbenem Doppelrand.
- Optische Reihenfolge aus Vorlage: Like, Dislike, Trennraum, Vorheriges, 10 s zurück, Pause/Play, 10 s vor, Untertitel CC, Info, Trennraum, Vollbild, HD. Zusätzliche Funktionen nur dezent und ohne Verschieben der visuellen Hauptgruppe.
- **Unterhalb der Leiste**: eine Reihe von sechs scharfen abgerundeten 16:9-Videovorschauen, je mit Titel und Laufzeit-Badge. Keine abgeschnittenen quadratischen Fokusrahmen.
- Uhr rechts oben in Weiß; technische Bitrate, Videoformat und Streamingprotokoll **nicht ständig eingeblendet**, sondern über das Info-Icon abrufbar.

## Umsetzung und Review

- Android VectorDrawable und `GradientDrawable` statt Bitmap-Buttons oder GPU-Blur; Skalierung anhand dp und Bildschirmbreite, keine hartkodierten 1672-Pixel-Koordinaten. Abweichungen in Buttonform, Farbe, Icons, Zentrierung und Fokusrahmen nach **realem Screenshot auf Fire TV/Thomson** beurteilen.
- Media3-/SABR-Decoder nicht wegen des Designs mehrfach instanziieren.
- UI-Komponente: `mt2-player-lab/MarsTvShell.java`, Drawables: `mt2-player-lab/assets/drawable/mars_tv_ic_*.xml`.
- **Geräteabnahme noch offen**. Die Implementierung von 0.0.11 ist eine Annäherung an das Zielbild; erst visueller Vergleich + D-Pad-Test bestätigt Erfüllung.
