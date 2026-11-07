# MT2-031 – APK-Größenaudit und risikoarme Optimierung

## Verifizierte Grundlage (MarsTube 0.0.30)

Die signierte APK wurde als ZIP-Archiv analysiert (keine Veränderung am Paket).

| Bereich | Komprimierte ZIP-Einträge |
| --- | ---: |
| Native Bibliotheken für ARM32 + ARM64 | 51.441.600 Bytes |
| DEX-/Programmcode | 12.538.094 Bytes |
| Android-Ressourcen (res/ und resources.arsc) | 5.111.630 Bytes |
| Andere ZIP-Einträge | 203.846 Bytes |
| **Gesamte signierte APK-Datei** | **69.560.896 Bytes / 69,56 MB** |

Die Datei umfasst zusätzlich ZIP-Metadaten, Alignment und APK-Signatureinträge;
darum ist die Summe der komprimierten ZIP-Einträge nicht exakt die APK-Dateigröße.

### Native Plattformanteile

- **arm64-v8a:** 30.296.144 Bytes
- **armeabi-v7a:** 21.145.456 Bytes
- **libj2v8.so** (über beide Architekturen): 35.296.560 Bytes
- **libcronet.143.0.7445.0.so:** 10.686.000 Bytes
- **libconscrypt_jni.so:** 4.725.608 Bytes
- **libglide-webp.so:** 733.432 Bytes

**Die nativen Dateien liegen unkomprimiert (ZIP_STORED) und aligned im APK.**
Das erlaubt Android, sie ohne zweite extrahierte Kopie direkt aus der APK zu
laden. Es erklärt den Unterschied zwischen der APK-Größe und der späteren
installierten Belegung.

### Technisch mögliche Optimierungen

1. **Architektur-spezifischer APK-Build (sicherste nennenswerte Einsparung):**
   Eine ARM64-APK könnte im Vergleich zur Universal-APK die ungenutzten 32-bit
   Bibliotheken weglassen (rechnerisch etwa 48,42 MB). Eine ARM32-APK könnte
   64-bit Bibliotheken weglassen (rechnerisch etwa 39,26 MB). Das sind *keine*
   bereits gebauten oder getesteten Release-Dateien. Zuerst tatsächliche ABIs
   auf dem Zielgerät auslesen und Installations-/Wiedergabetests durchführen.
   Die Universal-APK bleibt als Rückfalloption erhalten.
2. **R8 und shrinkResources:** im Upstream-Release deaktiviert.
   Testvariante separat bauen, DEX-/Ressourcen-Einsparung messen und Reflection,
   J2V8/JavaScript, Anmeldung, Media3/SABR, Provider, Updatepfad prüfen.
   R8 nicht ohne regressionssichere Tests in Produktion aktivieren.
3. **Komprimierte native Bibliotheken:** eine lokale DEFLATE-Simulation der
   bestehenden .so-Dateien zeigt rund 30 MB theoretisches APK-Potenzial,
   aber Android würde die .so bei der Installation extrahieren.
   Installierter Speicher kann daher *größer* werden; diese Option wird in
   0.0.31 nicht aktiviert.
4. **Unbenutzte Assets/Abhängigkeiten:** erst mithilfe der aktuellen
   Build-Abhängigkeitsstruktur und konkreter ZIP-Einträge prüfen. Keine
   Bibliotheken allein nach ihrem Namen entfernen.

### Freigabe für 0.0.31

0.0.31 liefert die kompakte Shorts-Übersicht sowie einen lokalen, reproduzierbaren
APK-Größenvergleich gegen 0.0.30. Aus Sicherheits- und Kompatibilitätsgründen
wird die Universal-APK in dieser Runde **nicht künstlich komprimiert**.
Die Berechnung von Einsparungen ersetzt niemals einen echten signierten Build
und einen Test am Fire TV bzw. Thomson TV.

Das Skript `mt2-player-lab/tools/Audit-MarsTube-Apk.py` liest beide lokal
gebauten APKs und zeigt die tatsächlichen Vorher-nachher-Werte. Es
schreibt keine Dateien und benötigt keinen Internetzugang.
