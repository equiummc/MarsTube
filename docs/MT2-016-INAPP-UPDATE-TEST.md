# MT2-016 – erster echter In-App-Update-Test auf dem Fire TV

## Ziel (05.11.2026)
MarsTube 0.0.15 soll nicht nur als GitHub-Actions-ZIP existieren. Die eingebaute **Einstellungen → Auf Updates prüfen**-Funktion soll ein **öffentlich veröffentlichtes GitHub-Release** entdecken, 0.0.15 herunterladen und Androids offiziellen Installationsdialog anbieten.

## Signaturproblem der bisherigen Testbuilds
Die lokal installierte 0.0.14 heißt `de.equiummc.marstube2.firetest014.debug`. Die separat gebaute 0.0.15 hat `de.equiummc.marstube2.firetest015.debug`, außerdem wechselnde CI-Debugsignaturen. Android **kann diese APKs nicht als Upgrade behandeln**.

## Exakte Testlösung
- Ein einziger CI-Job kompiliert **zuerst die eingefrorene 0.0.14-Oberfläche** und danach 0.0.15 im selben isolierten Build-Verzeichnis.
- Beide APKs tragen die **identische Android-Paketkennung** `de.equiummc.marstube2.updatelab.debug`, signiert mit **demselben kurzlebigen CI-Debug-Zertifikat**. Der Job prüft Paketkennung, `versionCode` 12014→12015 und SHA-256-Zertifikat beider APKs.
- 0.0.14 hat fest den HTTPS-Feed `https://github.com/equiummc/MarsTube/releases/download/mt2-015-updatelab-<CommitSHA12>/marstube-tv.json` eingebaut. Er akzeptiert keinen anderen Feed. Die JSON liefert nur die passende veröffentlichte 0.0.15-Update-APK, korrekte Dateigröße und `versionCode 12015`.
- Nur ein erfolgreicher Paarbuild löst den Release-Job aus: **GitHub-Prerelease** mit unveränderlichem Namen (nicht `latest`/Stable), inkl. `marstube-tv.json`, `MarsTube-0.0.15-update.apk` und SHA256SUMS.
- Testbasis und Update-APK als CI-Artefakt `marstube-014-to-015-update-test-pair`, eigene Ablage unter `dist/`.
- `tools/Install-MarsTube-014-UpdateBase.ps1` installiert nur die **0.0.14-Update-Testbasis**, nachdem SHA-256, APK-Paket, Versionscode und Amazon-Gerät `AFTR` überprüft wurden. Keine Deinstallation.
- Anschließend am TV **die Testbasis öffnen** und „Auf Updates prüfen“ wählen. Hier müssen Download, Installationsbestätigung und Versionswechsel 0.0.14 → 0.0.15 ohne ADB stattfinden.

## Konkreter Test am Fire TV
1. ZIP aus dem geprüften CI-Artefakt herunterladen und unentpackt in `Downloads` speichern.
2. Den PS7-Installationshelfer ausführen (Zielkennung `# MARS-ZIEL: PS7`).
3. Im Fire TV explizit die zusätzliche **MarsTube 0.0.14 (Update-Test)**-App öffnen.
4. **Einstellungen → Auf Updates prüfen** → erwartetes Angebot **0.0.15** → Download mit Fortschritt → Android-Dialog erlauben/bestätigen → App wieder öffnen.
5. **Einstellungen → Info** muss nun 0.0.15 anzeigen. Vorhandene Testbasis-Daten und Einstellungen sollen erhalten bleiben. Zurücknavigation und Short-Steuerung prüfen.
6. Offline, Download-Abbruch, unpassende Signatur dürfen niemals einen ungeprüften Installer starten.

## Grenzen
- Ein **kurzlebiger CI-Debugschlüssel** erlaubt exakt dieses Paar und ist **nicht für künftige Builds wiederverwendbar**. Nach Erfolg braucht MarsTube einen dauerhaft gesicherten Release-Keystore außerhalb des Git-Repositories (GitHub Actions Secret/Environments), eine stabile Paketkennung und kontrollierten Release-Feed. Niemals private Signierschlüssel in Git einchecken oder im Chat übertragen.
- Die Baseline ist bewusst **nicht** die bislang installierte `firetest014.debug`; sie ist ein isoliertes Testpaket. Dessen einmalige manuelle ADB-Installation ist erforderlich.
- Grüner GitHub-Build plus veröffentlichtes Release beweist nicht, dass der Fire-TV-Installer per D-Pad funktioniert. Endgültige Abnahme nur nach realem Gerätetest.

## CI
`.github/workflows/mt2-016-release-updatelab.yml` → Paarbuild, Paketprüfung, APKs, JSON, Prerelease.
