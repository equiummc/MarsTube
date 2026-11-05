# MT2-013 – Fire-TV-Gerätebefunde, Korrektur und Update-Test

## Beobachtungen aus realem Nutzertest der 0.0.12 (05.11.2026)

**Nicht abgenommen:** Home-Menü und Einstellungsmenü zu breit; Player-Rail blieb während laufender Wiedergabe links permanent sichtbar; Steuertasten stilistisch okay, aber wesentlich zu groß; Shorts-Seite ließ sich öffnen, jedoch konnte der Fokus nicht von der Rail auf die Shorts-Kacheln wechseln; BACK/Zurück hatte keine nachvollziehbare Navigation. Nutzungsbereitschaft daher unzureichend.

## Umsetzung in 0.0.13 (Code, noch kein erfolgreiches reales Gerätetest-Ergebnis)

- **64 dp einheitliche linke TV-Rail** in Browse, Suche, Einstellungen und Player; Icons ohne überlange Textzeilen. Rechte Seite erhält entsprechende Inhaltsreserve. `MarsTvRail.java` und `MarsTvShell.java`.
- **Player-Rail nur bei ausdrücklichem Fokus `RAIL` sichtbar**, sonst freie Videofläche. Keine zweite dekorative oder dauerhaft sichtbare Videonavigation.
- **Steuerknöpfe 48→32 dp, Play/Pause 58→38 dp**, Leistenhöhe 56→44 dp, reduzierte Innenabstände. Fokus weiterhin sichtbar, scharf und D-Pad-kompatibel.
- **BACK in Player**: Info-Overlay schließen, sonst mit *einem* Tastendruck zur Browse-Ansicht und optional laufendem Miniplayer. Keine verzweigte `railEverOpened`-Mehrfachtastenregel mehr.
- **Fokus Shorts**: Bei rechter Pfeiltaste aus der Rail direkte programmatische Anwahl des ersten Video-Kacheleintrags; Auswahlindex bei Kategorienwechsel zurücksetzen; asynchronen Short-Feed kurz begrenzt nachprüfen, falls er erst nach dem Navigationsevent geladen wird. BACK von Shorts → Home, bei Home → Rail. Keine Versuche, unsichtbare/noch nicht vorhandene Karten anzuklicken.
- Die neue Updater-Funktion aus 0.0.12 bleibt enthalten.

## Zwei verschiedene Testpakete – bitte nicht verwechseln

1. **0.0.13 als unabhängige Optik-/Bedien-Test-APK:** `de.equiummc.marstube2.firetest013.debug`, parallel zu 0.0.12. Kann nicht per In-App-Update von `firetest012.debug` installiert werden, weil Paket-ID und Debug-Zertifikat anders sind.
2. **Update-Lab-Paar:** `de.equiummc.marstube2.updatelab.debug`, in **einem CI-Job** als 0.0.12 Basis + 0.0.13 Update mit **demselben temporären Debug-Schlüssel** gebaut. Die Baseline einmalig per ADB installieren, 0.0.13 danach in MarsTube über **Einstellungen → Info → Auf Updates prüfen** installieren. Diese temporäre Testidentität ist bewusst **kein** dauerhafter Stabilkanal. Ein erneuter CI-Run erzeugt einen anderen Schlüssel; eine fremde Pair-APK darf nie als Upgrade darüber installiert werden.

## Infrastruktur

- Eigenständiger Workflow: `.github/workflows/mt2-013-updatelab.yml`.
- Test-Release **nur nach erfolgreich geprüfter Bau-Paarung** als eigenes öffentliches GitHub **Prerelease** mit unveränderlichem Tag `mt2-updatelab-<GitSHA12>`; keine Berührung des `latest`-Stable-Feeds.
- Der Test-Feed `marstube-tv.json` und das 0.0.13-APK liegen auf **demselben getaggten Release**, die App akzeptiert nur exakt diesen Feed und prüft zusätzlich **gleiche Paketkennung + Versionsanstieg + Signatur** vor Übergabe an Androids offiziellen Installationsdialog.
- CI-Artefakt `marstube-v013-update-lab-pair` mit Baseline/Upgrade, `SHA256SUMS` und Buildnachweis.
- Windows-Installationshelfer: `tools/Install-MarsTube-013-UpdateLab.ps1` liest die unentpackte ZIP direkt aus dem Downloads-Ordner, prüft Hash und Gerätekennung (Amazon AFTR), installiert **nur das eigene Update-Lab-Paket**. Das bereits installierte `firetest012` bleibt erhalten.
- Die Schlüssel sind ausschließlich für **ein einmaliges Testpaar** gültig. Für echte MarsTube-Updates nach 0.0.13 muss ein dauerhafter Signierschlüssel sicher verwaltet werden, ohne ihn auf GitHub öffentlich zu hinterlegen.

## Akzeptanztests – noch nicht bestanden, Usertest nötig

- [ ] Fire TV: Home-Rail und Einstellungen optisch wirklich schmal, konsistent, keine überbreite zweite Navigationsleiste.
- [ ] Player: linke Rail verschwindet ohne Aktivierung; Bedienelemente ca. ein Drittel kleiner; zentriert und weiterhin gut fokussierbar.
- [ ] BACK einmal aus normaler Wiedergabe zur Browse-Ansicht, mit aktivem Miniplayer zurück über „Laufendes Video“, ohne unerwarteten Stopp.
- [ ] Shorts-Menü öffnen → rechts → erste Kachel → OK → Video startet; Pfeil runter/nach oben nur nächste/vorige Shorts.
- [ ] Fokuswechselsicherheit bei leerem/ladendem Feed, wechselnder Kategorie, Suche, Einstellungen.
- [ ] 0.0.12-Lab-Basis installieren; Updateprüfung zeigt exakt 0.0.13; Download, Fortschritt, sichtbarer Android-Installationsdialog, Bestätigung, Versionswechsel ohne verlorene Einstellungen.
- [ ] Bei Offline/Abbruch, falscher Signatur und anderem APK-Paket keinen fremden Installer öffnen.
- [ ] Thomson-Tauglichkeit und Media3/SABR-Regressionsprüfung.

**Nicht behaupten, dass diese Punkte bereits auf dem Gerät bestätigt sind.**
