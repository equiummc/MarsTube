# MT2-039: Nur ARM32 für Fire TV Cube

## Warum
Der vorherige 0.0.38-Build wurde trotz ausdrücklicher Fire-TV-32-Bit-Abstimmung
als Universal-APK mit armeabi-v7a **und** arm64-v8a erstellt (66,37 MiB).
Die zweite Architektur wird vom Amazon AFTR (raven) mit ABI armeabi-v7a
nicht benötigt. Dies ist ein Fehler in der Ausgabekonfiguration, keine
Anforderung nach einer weiteren App.

## Was geändert wurde
- Gradle-App-Modul vor der APK-Erstellung auf **ndk abiFilters 'armeabi-v7a'**
  gesetzt; **ABI-Splits deaktiviert**. Android Gradle Plugin verbietet die
  gleichzeitige Aktivierung von ABI-Splits und ndk-ABI-Filtern. Es wird nur
  eine APK mit genau einer nativen Architektur (ARM32) gebaut.
- Neue Build-Version 0.0.39/code 12039, damit ein **Update über die
  bisherige 0.0.38 ebenso wie über 0.0.36** sicher möglich ist.
- **Unverändert**: applicationId `de.equiummc.marstube2`,
  dauerhafter GitHub-Key, Benutzerkonten, App-Daten, Funktionsstand A+B+C,
  100.000er-Profile-Negativliste, Live, Musik.
- Die APK wird vollständig über Gradle gebaut und mit dem Originalschlüssel
  signiert. Keine nachträgliche APK-Bearbeitung, kein zipalign nach Signatur.
- GitHub CI prüft Java-/Media3-/SABR-Tests, SHA-256, Paket, Version und die
  **exakte** ursprüngliche Zertifikatskennung.
- ZIP-Inhaltsprüfung: alle `lib/**/*.so` müssen unter
  `lib/armeabi-v7a/` liegen, keine ARM64-/x86-Bibliotheken.
- PowerShell-7-Installer enthält denselben ARM32-Test, die unveränderte
  Signaturprüfung, den Schutz vor Downgrades und nur `adb install -r`.
  Keine automatische Installation/Deinstallation.

## Fire-TV-Abnahme
1. 32-Bit-APK vom erfolgreichen CI-Artefakt holen, keine ältere Universal-APK.
2. Signatur/Version/ABI mit dem mitgelieferten PS7-Installer prüfen.
3. Vorhandene MarsTube-Version wird ohne Deinstallation aktualisiert.
4. Profilwechsel, Live-/Musiknavigation, Shorts und Video-X erneut testen.
5. Kein Performance-Versprechen allein durch kleinere APK: weniger Download
   und weniger verteilte Bibliotheksbytes, Laufzeitnutzen muss gemessen werden.

## Scope-Regel für weitere Builds
**Alle Fire-TV-Testversionen ab diesem Stand bleiben armeabi-v7a-only**, bis
der Nutzer ausdrücklich die Unterstützung einer weiteren Architektur
freigibt. Der Build muss bei einer späteren Universal-/ARM64-Regression
abbrechen, statt unbeabsichtigt eine größere Testdatei auszuliefern.
