# MT2-012 – Updates direkt auf Android TV

**Stand: 05.11.2026 – implementierter Entwicklungszweig, Geräteabnahme offen.**

## Ziel
Einmalig eine eigene MarsTube-Updatebasis installieren. Danach soll die nächste eigene MarsTube-Version direkt im TV-Menü über **Einstellungen → Info → Auf Updates prüfen** erscheinen, auf dem Fernseher heruntergeladen und nach ausdrücklicher Zustimmung mit Androids System-Installer aktualisiert werden. Konto, Einstellungen und Verlauf bleiben bei einem echten Paketupdate erhalten.

## In v0.0.12 umgesetzt
- Der bisher **funktionslose** Eintrag „MarsTube-Updates“ in `MarsTvSettingsPages` startet nun `MobileUpdateActivity.startCheck()`.
- Die eingebundene Update-Komponente `AppUpdates` wurde auf den **eigenen** HTTPS-Feed begrenzt: `https://github.com/equiummc/MarsTube/releases/latest/download/marstube-tv.json`. Keine NewTube-/SmartTube-Updatequelle und kein frei wählbarer Debug-Override.
- Die bestehende Updateoberfläche zeigt Versionsstand, Änderungsnotizen, Downloadfortschritt, Fehler, Abbruch und die Schaltflächen „Später“ bzw. „Aktualisieren“. TV-Buttons erhalten einen eindeutigen D-Pad-Fokus.
- `MarsUpdateApkGuard` kontrolliert vor dem Android-Installationsdialog: APK-Datei vorhanden, Paket-ID **identisch** zum laufenden MarsTube-Kanal, gemeldeter Versionscode **identisch** zum APK, Versionscode größer als installiert und das APK mit **demselben Zertifikat** signiert. Bei Fehler: keine Installation.
- `REQUEST_INSTALL_PACKAGES` ist nur für die Updatebasis erlaubt; Android muss die Installation und gegebenenfalls diese App als Installationsquelle erlauben. Keine heimlichen Installationen, keine Deinstallationen.
- Isolierte aktuelle Test-App: `de.equiummc.marstube2.firetest012.debug` mit Version `0.0.12`, Code `12012`. 0.0.9/0.0.10/0.0.11 bleiben separat bestehen.

## Noch nicht erfüllt / Voraussetzungen für einen echten Updateversuch
1. **0.0.12-Build samt CI-Tests muss erfolgreich sein**, und 0.0.12 muss auf Fire TV und Thomson die Updateoberfläche tatsächlich korrekt rendern.
2. Das MarsTube-Repository besitzt bislang **keine veröffentlichte GitHub Release** mit `marstube-tv.json` und einem passenden 0.0.13-APK. Vor einer Veröffentlichung zeigt die Updateprüfung daher einen Fehler, nicht „kein Update verfügbar“.
3. Das GitHub-CI-**Debug-Zertifikat ist nicht dauerhaft an denselben privaten Schlüssel gebunden**. Eine neu gebaute 0.0.13-APK mit einem anderen Debug-Zertifikat kann die aktuell installierte 0.0.12 nicht aktualisieren. Eine **stabile Signieridentität muss vor der erstmaligen endgültigen 0.0.12-Installation eingerichtet** werden; privaten Schlüssel niemals im Repository, in Logs oder öffentlich sichtbaren Artefakten speichern.
4. Für den **ersten echten 0.0.12 → 0.0.13-Test** dieselbe Paketkennung `de.equiummc.marstube2.firetest012.debug` beibehalten, Versionscode erhöhen (`12013`) und **dasselbe Zertifikat** verwenden. Andernfalls erfolgt keine Aktualisierung. Die endgültigen stabilen/Alpha-Kanäle erhalten später jeweils eigene konstante Paketkennungen.
5. APK-Download läuft über HTTPS und wird vom Android-Installationssystem auf Signaturgültigkeit geprüft; `MarsUpdateApkGuard` prüft Paket und Zertifikat zusätzlich. **Ein SHA-256-Vergleich gegen einen Release-Manifest-Digest ist noch nicht implementiert** und gehört vor einer öffentlichen Release in die Sicherheitsabnahme.
6. Die Updateoberfläche stammt technisch aus einer mobilen Bottom-Sheet-Komponente, die nun TV-Fokus erhält. Vollständig maßgeschneiderter MarsTube-TV-Dialog, Tests auf Fire TV/Amazon-Installationsbeschränkungen und Verhalten bei fehlender Release-Quelle sind noch offen.

## Release-Manifestformat
Die bestehende `AppUpdateChecker`-Bibliothek erwartet ein JSON-Objekt mit einer `package`-Sektion und einer Sektion je Versionsnummer, z. B. **nur schematisch, nicht als veröffentlichter Feed**:

```json
{
  "package": {
    "downloadUrlList": ["https://github.com/equiummc/MarsTube/releases/download/v0.0.13/MarsTube-0.0.13-tv.apk"],
    "downloadSize": 81000000
  },
  "0.0.13": {
    "versionCode": 12013,
    "changelog": ["Updates direkt auf dem Fernseher", "Verbesserte TV-Navigation"]
  }
}
```

`downloadUrlList` muss auf eine tatsächlich bestehende, zur installierten Paketkennung passende, korrekt signierte APK führen. Die oben genannten Dateinamen/Größen sind nur Beispiele und dürfen **nicht ohne überprüften Build** veröffentlicht werden.

## Gerätetests (alle noch offen)
- [ ] Auf Fire TV unter Info „Auf Updates prüfen“ per Fernbedienung öffnen; klarer Fokus/Zurück-Navigation.
- [ ] Ohne veröffentlichten Feed: sichtbare Fehlermeldung, kein Crash.
- [ ] Mit Test-Release: Changelog, Größe, Downloadfortschritt, Abbruch, Später.
- [ ] Falsche Paket-ID, falsche Signatur und Downgrade werden vor Installationsdialog abgewiesen.
- [ ] Echte 0.0.12 → 0.0.13 Aktualisierung mit **demselben Zertifikat**: Konto/Einstellungen bleiben erhalten.
- [ ] Androids Dialog zur Bestätigung der Installation sichtbar, keine stillschweigende Installation.
- [ ] Gleiche Prüfung auf Thomson und Sony, soweit unterstützt.

**Hinweis an Coding Agents:** Eine CI-grüne Debug-APK beweist noch **kein** funktionierendes In-App-Paketupdate. Solange Schlüsselkontinuität und ein echtes Release fehlen, nicht als „Updatefunktion fertig und erfolgreich getestet“ markieren.
