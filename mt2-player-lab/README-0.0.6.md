# MarsTube 2.0.6: Eine Engine, zwei Bedienoberflächen

## Auftrag

Das fertige Produkt heißt **MarsTube**, nicht NewTube, MeTube oder YouTube. 
YouTube ist ausschließlich der Videodienst, nicht der App-Hersteller oder 
ein Updatekanal. Wir verwenden die Open-Source-Komponenten nur unter 
fortgeltenden Lizenzen und Hinweisen.

**Originale MarsTube-1.x-App bleibt installiert und unverändert.**
**MarsTube 0.0.5 ist der bewährte schnelle Fire-TV-Prototyp.**
**MarsTube 0.0.6 kombiniert diese Basis mit einer passenden Handy-Ansicht.**

Die TV-Native-Codebasis liegt in `mars/stable` und ist eine Quelle für 
bewährte D-Pad-Navigation, TV-Menüführung und kompatible Komponenten. 
Die Referenzversion für Media3 + SABR bleibt beim geprüften Commit von 
`aleixrodriala/newtube`. **Es handelt sich bis zur vollständigen 
Integration um einen offenen Prototyp – nicht um ein vollständig
unabhängig umgebautes MarsTube-Release.**

## Architekturentscheidung

| Ebene | Gemeinsame Basis | Fernseher | Smartphone |
|---|---|---|---|
| Streaming | YouTube-Metadaten / SABR-Fallback / DASH | gleich | gleich |
| Wiedergabe | Media3 ExoPlayer | Hardwaredecoder | Hardwaredecoder |
| Start / Listen | gemeinsame Presenter/Daten | D-Pad, Fokusmarkierung | Touch, Scrollen |
| Video-Bedienung | gleicher Player/Playhead | MarsTvShell, 6 Videos, 16:9, blaues M im Menü | Originale Touch-Controls und Miniplayer |
| Einstellungen | dieselben Werte | per D-Pad auswählbar, native TV-Verbesserung folgt | Touch-Seiten |
| Sprache | deutsche Ressourcen mit Umschaltoption | Deutsch bei Erstinstallation | Deutsch bei Erstinstallation |
| Branding | Name `MarsTube` + eigenes M-Logo | Leanback Launcher | Standard Android Launcher |
| Update | keine fremde Quelle | lokale APK | lokale APK |

Erkennung über `UiModeManager.UI_MODE_TYPE_TELEVISION` oder 
`PackageManager.FEATURE_LEANBACK`, niemals über Bildschirmgröße.
Dadurch bleibt ein Android-Handy auch im Querformat touchfähig.

## Konkrete Änderungen im Patch 006

1. `MarsDeviceMode.java` leitet das UI vom Gerät ab.
2. `MobilePlaybackActivity` erzeugt die vorhandene MarsTvShell nur
   auf echten Fernsehern; auf dem Handy funktioniert das Original-Touch-UI.
3. Deutsch wird bei einer **frischen** Installation als Startsprache
   gespeichert, bereits vom Nutzer geänderte Sprache bleibt später erhalten.
4. Der Infobereich zeigt MarsTube-Versionsnummer und unser GitHub-Repository.
   Er verlinkt weiterhin die Open-Source-Lizenzinformationen.
5. Ein Updater für NewTube/andere Paketnamen wird nicht aktiviert.
   Bis zur eigenen signierten Updatequelle erfolgt die Installation per APK.
6. Paketkennung `de.equiummc.marstube2.beta.debug` (Debug-Build),
   Version `0.0.6`. Eine *universelle* APK enthält `armeabi-v7a`
   für Fire TV Cube und `arm64-v8a` für aktuelle Android-Telefone/TVs.

**Wichtig für Updates:** Debugsignaturen aus verschiedenen CI-Durchläufen
können abweichen. Ein echtes in-place Update ohne Deinstallation ist
erst mit festgelegtem MarsTube-Keystore und gleichbleibender Paketkennung
garantierbar. Deshalb neue Builds vorläufig parallel installieren,
nicht die vorhandene 1.x-App ersetzen.

## Was wir aus MarsTube 1 noch übernehmen

- Bewährte D-Pad-Fokuspfade in allen Untermenüs und Kontrollleisten.
- Klare TV-Menüstruktur und deutsche Bezeichnungen aus
  `common/src/main/res/values-de/strings.xml`.
- Zuverlässige BACK-Rückführung an die vorherige Einstellungsebene,
  zum Video bzw. zur Startseite.
- TV-spezifische Einstellungen: Player, Videoqualität/Decoder, Audio,
  Untertitel, Oberfläche, Wiedergabeverlauf, Downloads, Info.

Diese Übernahmen sind noch **keine** Behauptung, dass die gesamte
Leanback-Implementierung bereits in Media3 integriert ist.

## Abnahme vor Freigabe

- [ ] Fire TV Cube AFTR: Start aus TV-Launcher, Uhr/M nur bei Menüs,
      Navigation LINKS/RECHTS/HOCH/RUNTER/OK und zweimal BACK.
- [ ] Fire TV Cube: 6 sichtbare Vorschauen, 16:9, zweizeilige Titel,
      Bild- und Tonwiedergabe schnell und ohne unnötige Decodierung.
- [ ] Fire TV Cube: im Miniplayer Fokus erreichbar, Rückkehr zum
      laufenden Video ohne App-Abbruch oder fremde Startansicht.
- [ ] Android-Handy: APK installieren, Hochkant/Querformat,
      touchfähige Video-Controls, Suche, Einstellungen, Miniplayer.
- [ ] Thomson Android 14 + weiterer Google TV: D-Pad
      und Hardware-Codec-Kompatibilität.
- [ ] Deutsche Menüs (inkl. Unterseiten), keine sichtbare NewTube-Wortmarke.
- [ ] Info: MarsTube-Version stimmt; keine Fremd-APK installierbar.
- [ ] Echte Open-Source-Attribution und Lizenztexte bleiben erhalten.
- [ ] Eigene Keystore-/Release-Pipeline für spätere Updates.
- [ ] Danach Shorts-Integration in Feed und Player.

**Build-Gate:** `mt2-media3-sabr-lab.yml` patcht ausschließlich einen
gepinten, Wegwerf-Checkout. Der produktive `mars/stable`-Zweig wird nicht
verändert. Buildstatus ist kein Hardware-Abnahmetest.
