# MarsTube 2.0 – verbindlicher Produktionsstand

Stand: 05.11.2026

## Eine App statt paralleler Test-APKs

Ab **0.0.16** wird ausschließlich die feste Android-Application-ID
`de.equiummc.marstube2` für die weiterentwickelte MarsTube-2-App verwendet.
Kein `firetestXXX`, `updatelab`, versionsabhängiger Paketname oder Debug-Suffix.
Die bestehenden Altinstallationen werden nicht automatisch entfernt.

Die bisherige 0.0.15 ist eine separate Testinstallation mit anderer Paketkennung
und kurzlebiger CI-Signatur. Sie lässt sich nicht einfach in die Produktions-App
umwandeln. **Genau eine weitere Neuinstallation ist nötig** – danach nur Upgrades.

## Dauerhafte Signatur

- Ein einziger Android-Release-Keystore, ausschließlich beim Eigentümer gespeichert.
- GitHub Actions erhält den verschlüsselten Secret-Transport über die vier
  Repository-Secrets `MARS_SIGNING_KEYSTORE_B64`, `MARS_SIGNING_STORE_PASS`,
  `MARS_SIGNING_KEY_PASS`, `MARS_SIGNING_KEY_ALIAS`.
- Der Keystore / seine Passwörter dürfen niemals in Git oder einem Chat liegen.
- Vor jeder Veröffentlichung wird die Signatur der fertigen APK geprüft.
- Benutzer muss Keystore und Passwort separat sichern. Verlust verhindert Updates.

## Versionierung, Build und OTA

- Aktive Quelle: `mars/production-2`
- `mt2-player-lab/production-version.json` ist die Releaseversions-Quelle.
- Build-Pipeline: `.github/workflows/mt2-production-release.yml`.
- Bau-Skript: `tools/build-marstube-production.sh`.
- Zur Veröffentlichung wird `v<version>` als **reguläres, nicht als Prerelease**
  markiertes GitHub-Release veröffentlicht; APK `MarsTube-<version>.apk`
  mit `marstube-tv.json`.
- Der in der App hinterlegte Kanal lautet:
  `https://github.com/equiummc/MarsTube/releases/latest/download/marstube-tv.json`.
- Update-Schutz prüft Paketkennung, steigenden Versionscode und Signatur.
- Bereits veröffentlichte Versions-Tags oder APKs dürfen nicht überschrieben werden.

## Geräteinstallation

- Ersteinrichtung erfolgt über das lokale Skript
  `Install-MarsTube-Produktion.ps1` mit GitHub CLI und Java/Android SDK.
- Einmalige Installation der normal signierten APK auf dem Fire TV Cube AFTR;
  danach Updates ausschließlich per MarsTube-Menü.
- Die alten Paketkennungen werden nur nach bestätigtem Start und
  ausdrücklichem **JA** des Eigentümers deinstalliert.
- Die TV-Oberfläche muss weiterhin auf Fire TV und Thomson geprüft werden.
- Ein grüner Build allein ist kein erfolgreicher Geräte-Update-Test.

## Status am 05.11.2026

Produktionsidentität, Releaseskript, Workflow und aktueller Code sind auf GitHub
angelegt. **Die vier permanenten GitHub-Signing-Secrets sind noch nicht eingerichtet;
daher ist aktuell kein signiertes Produktions-APK veröffentlicht.**
Keine Behauptung einer fertigen App, bevor Build und Release erfolgreich sind.
