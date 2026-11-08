# MarsTube – Verbindliche Architektur- und Geräteplanung (08.11.2026)

## Eine App, drei APK-Pakete

Das Projekt verwendet weiterhin genau **eine** Paketkennung (`de.equiummc.marstube2`), einen Release-Schlüssel und **eine** fortlaufende Versionsnummer. Keine TV- oder Smartphone-Fork-App anlegen.

| Name | Native ABI(s) | Verwendung |
| --- | --- | --- |
| `MarsTube-<VERSION>-ARM32.apk` | `armeabi-v7a` | Fire TV Cube, Thomson Android 14, Sony Android 10; ältere 32-bit-Geräte |
| `MarsTube-<VERSION>-ARM64.apk` | `arm64-v8a` | Pixel 8 und andere native ARM64-Smartphones/Android-Geräte |
| `MarsTube-<VERSION>.apk` | `armeabi-v7a` + `arm64-v8a` | Universal-Fallback, unbekannte Geräte und Kompatibilitätstest |

Ein ARM64-fähiger SoC **garantiert kein ARM64-Android**. Vor Installation `ro.product.cpu.abilist` prüfen; `ro.product.cpu.abilist64` kann zur Absicherung gelesen werden. Bei beiden ABIs ARM64 priorisieren, sofern der Anbieter vollständige native ARM64-Bibliotheken mitliefert.

Die Architekturvarianten enthalten denselben Java-/Kotlin-/Media3/SABR-Code, dieselben Ressourcen, dieselbe App-ID, VersionCode und signierende Identität. Unterschieden werden ausschließlich enthaltene `.so`-Bibliotheken. Keine Feature-Schalter, die je Variante divergieren.

## Reihenfolge

1. **Priorität TV:** ARM32 für Fire TV Cube, Thomson und Sony bauen; vorhandene 0.0.31-Universal-APK unverändert lassen. Installation per `adb install -r` nur nach Prüfung von APK-Signatur, Paketkennung, VersionCode und ABI.
2. **TV-Abnahme:** Shorts-Vorschauen, Creator-Blau, Fernbedienung, Zurück-Taste, Media3/SABR, Suche/Shorts, Boot/Resume und Updates auf Fire TV zuerst prüfen; danach Thomson (wenig RAM), Sony (Android 10).
3. **ARM64 vorbereiten:** ARM64-Signierbuild als technische Variante. **Noch keine mobile UI-Freigabe.** Pixel 8 für spätere Touchscreen-Tests vormerken.
4. **Mobile UI separat:** Displaygrößen, Portrait, Wischgesten, Touch-Navigation und andere Ressourcen/Bildelemente als gesonderte Entwicklung mit derselben Codebasis; bestehende TV-Steuerung nicht verändern.
5. **Veröffentlichung/Updater:** Bei drei Dateien muss der integrierte Updater anhand `Build.SUPPORTED_ABIS` die passende URL und Hash/Signatur auswählen und bei fehlender Variante sicher auf Universal zurückfallen. Solange das nicht getestet ist, bleibt die Universal-Datei der sichere Standard für appinterne Updates. **Nicht drei verschiedene Apps veröffentlichen.**

## Größen- und Speichermessung

Universal 0.0.31: ca. 69,6 MB. ARM32 ca. 39,3 MB, ARM64 ca. 48,4 MB sind **berechnete Zielgrößen**, keine verifizierten finalen Varianten.

Die native Bibliotheksgröße reduziert den APK-Footprint auf dem Gerät und die übertragenen Bytes bei vollständigen Sideload-Updates. Das ist **kein** inkrementelles Delta-Update: der jeweilige APK-Download bleibt vollständig. RAM, Hardware-Decoding und Startzeiten sind nicht allein aufgrund des ABI-Zuschnitts messbar besser.

Messung: signierte Dateigröße, tatsächlich vorhandene `lib/<ABI>`-Dateien, vom Package Manager gemeldete App-/Daten-/Cache-Größe vor/nach Installation, Wiedergabe und Startzeit. App-Daten nicht löschen; keine Deinstallation.

## Buildstatus und Vorsicht

- Universal 0.0.31 ist in GitHub CI bereits erfolgreich signiert und geprüft.
- ARM32-Quell-/PowerShell-Starter besitzt eine erfolgreiche Windows-PS7-Syntaxprüfung; der vollständige GitHub-Build war zuletzt durch HTTP-504 beim externen Gradle-9.6.1-Download blockiert, nicht durch einen nachgewiesenen ABI-Fehler.
- ARM64-PS7-Starter ist separat im Repository; mobile Bedienung und nativer ARM64-Build sind **noch nicht end-to-end getestet**.
- `E:\MarsTube` und der aktive lokale Branch dürfen nicht überschrieben werden. Varianten in isolierten Build-Verzeichnissen erzeugen. Original-Key nur lokal sicher nutzen; keine Credentials committen.
