# Mars SmartTube – Shadow Fork (Entwicklungsgrundgerüst)

Quelle: `https://github.com/yuliskov/SmartTube` (MIT für das Hauptprojekt; Submodule haben eigene Lizenzen).

**Ziel:** SmartTube mit eigenem Package `de.equiummc.mars.smarttube`, Mars TV Dark Theme und einem deutlich sichtbaren Update-Hinweis. Die Update-Rubrik bleibt angepinnt und die App zeigt beim Start einen Update-Dialog, sobald der eingebaute Downloader eine neue eigene Version erkennt. Die App verwendet ausschließlich **eigene** Update-URLs statt Original-SmartTube-APK-Dateien. Die Android-Paketinstallation muss vom Nutzer bestätigt werden.

## Entwicklungsstatus

Dieses Gerüst erstellt einen öffentlichen GitHub-Fork mit separatem Produktbranch `mars/stable`. Es ist **keine** fertige, gebaute, signierte oder getestete APK. Die tägliche GitHub Action erkennt offizielle neue stabile SmartTube-Releases und öffnet ein Review-Issue. Sie installiert nichts auf Fernsehern und merged keinen fremden Code ungeprüft. Neue Releases werden auf einer Review-Branch geprüft, nach dem Build auf beiden Geräten getestet, danach separat signiert freigegeben.

## Erste Schritte

1. Start-MarsSmartTube.ps1 unter Windows ausführen. Erforderlich: `git`, GitHub CLI (`gh auth login`), Python 3, Android SDK zum Bauen.
2. GitHub Actions auf dem Fork ggf. aktivieren und Schreibrechte für Issues geben.
3. Build zunächst lokal mit `gradlew.bat :smarttubetv:assembleStstableDebug` prüfen. Noch nicht produktiv installieren.
4. Vor dem ersten Release **einen eigenen langlebigen Signaturschlüssel** erstellen; `keystore.properties` nicht einchecken. Entwickler-APK und produktive APK unterscheiden.
5. Erst nach erfolgreichem Build und Test auf beiden Geräten `mars/scripts/Publish-MarsRelease.ps1` verwenden. Das Skript baut und prüft eine signierte Release-APK, veröffentlicht sie über GitHub Releases und aktualisiert das öffentlich abrufbare Feed auf `master`. Die Build-/Releasestruktur des Forks muss zuerst real getestet werden.

## Updates auf dem Fernseher

Weil der offizielle AppUpdateChecker eine APK bereits beim Prüfen herunterlädt, bleibt die sichtbare Update-Mitteilung auf einen verfügbaren und erfolgreich heruntergeladenen Release beschränkt. Dauerhaft eingeblendet bleibt die Update-Rubrik **solange ein Update verfügbar ist**. Ein neues Release wird nie ohne Bestätigung durch Android installiert.

## Sicherheitsregeln

- Kein automatisches Übernehmen ungeprüfter Android- oder Netzwerkänderungen.
- Keine fremden APK-Signaturen übernehmen oder unbekannte APKs aus Apps-Stores installieren.
- Eigene Paket-ID und eigene Signatur behalten; niemals den privaten Schlüssel in GitHub committen.
- Fork ist wegen der öffentlichen Originalquelle ebenfalls öffentlich.
- Upstream MIT und Submodul-Lizenzen/NOTICE-Dateien unverändert behalten.
- Vor jedem Release beide TVs testen und prüfen, ob ursprüngliche Update-URLs noch irgendwo im Build aktiv sind.
