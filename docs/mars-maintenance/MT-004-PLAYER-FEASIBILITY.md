# MT-004 · Player-Performance und Upstream-Kompatibilität

**Stand:** Quellcode-Audit 2026-10-31. Baseline: MarsTube
`feature/mt-001-upstream-ci-20261031`, verglichen mit offiziellem
SmartTube-Stable-Tag `32.68s`.

**Freigabestatus:** **Machbarkeit bejaht, keine Player-Änderung freigegeben.**
Dies ist eine Diagnose und ein Prüfplan, kein Benchmark oder APK-Release.

## Vorrangige Ziele

1. Flüssige Wiedergabe und stabile Fernbedienungssteuerung **vor**
   transparenter Oberfläche, Animationen und zusätzlichen Grafikeffekten.
2. Primäre Testgeräte: **1,5 GB und 2 GB RAM**; Android 8/API 26 oder neuer.
   Gerätedaten nach nutzbarem RAM, API-Level und Hardware-Decoder erfassen.
3. Bevorzugt kleine, reversible Änderungen und messbare Vorteile; kein
   Plattformwechsel aus Prinzip.
4. Neue SmartTube-Versionen müssen weiterhin mit vertretbarem Aufwand
   übernommen werden können.

## Bestätigter Ist-Zustand des Players

- MarsTube nutzt einen **angepassten Amazon-ExoPlayer-2.10.6-Unterbau**
  in `exoplayer-amzn-2.10.6/`, mit zusätzlichen Stream-Formaten und
  Player-Anbindungen. `ExoMediaSourceFactory` unterstützt DASH, HLS
  und SABR, sowie Standard-HTTP, OkHttp und Cronet, abhängig von der
  aktuell gewählten Konfiguration.
- `ExoPlayerInitializer` besitzt vier Videopuffer-Profile.
  Standard `BUFFER_MEDIUM` verwendet 30 s Pufferziele und eine
  Wiedergabestartschwelle von 2,5 s. `BUFFER_HIGHEST` begrenzt das
  Pufferziel anteilig mit `deviceRam / 18`; die Sonderbehandlung gilt
  **nicht pauschal für alle Pufferprofile**. Nicht vorschnell den
  Gesamtpuffer verkleinern, da dies neue Nachladeruckler auslösen kann.
- `VideoLoaderController.preloadNextVideoIfNeeded()` lädt in den
  letzten 50 s eines Videos Metadaten/Formate für den nächsten Titel.
  Ob dadurch RAM- oder Netzlast entsteht, muss vor Änderungen gemessen
  werden.
- Bereits vorhanden: `PlaybackStartupMetrics` mit Phasen
  Videoauswahl → Formatanfrage → Formatantwort → Player-Vorbereitung →
  Player bereit → erstes Bild; Fehler werden ohne Video-ID, Titel oder
  Konto-URL protokolliert. Dieser Messpunkt ist der Startpunkt.
- Die Behandlung von SABR-Livestreams hat Mars-spezifische Ergänzungen
  in `ExoPlayerController` und `PlayerTweaksData` (z. B. Live-Position,
  eigene Einstellungen). Das sind potentielle Upstream-Merge-Konflikte.

## Technische Upstream-Vergleichsprüfung

Git-Blob-SHA-Abgleich gegen `yuliskov/SmartTube`, Tag `32.68s`:

| Datei / Modul | Gegenüber Upstream 32.68s | Konsequenz |
| --- | --- | --- |
| `ExoPlayerInitializer.java` | Identisch | Sehr guter Ausgangspunkt für Updatefähigkeit. Diesen Kern nach Möglichkeit unverändert lassen. |
| `ExoMediaSourceFactory.java` | Identisch | Stream-Quellen und Netzwerk-Transport nicht auf Verdacht forken. |
| `exoplayer-amzn-2.10.6/.../ExoPlayerFactory.java` | Identisch | Kein sofortiger Austausch des Engine-Kerns nötig. |
| `ExoPlayerController.java` | Modifiziert | SABR-Live-Unterstützung/Messpunkte schützen; Merge-Konflikte explizit prüfen. |
| `PlayerTweaksData.java` | Modifiziert | Upstream-Persistenzformat und lokale Settings nicht brechen. |
| `PlaybackStartupMetrics.java` | Mars-spezifisch | Messlogik möglichst isoliert halten. |
| `MediaServiceCore`, `SharedModules` | Andere Gitlink-Commits | Aktualisierung der Submodule separat testen; niemals blind auf Upstream-HEAD setzen. |

Vergleich sagt nur aus, ob die Datei-Inhalte in diesen Revisionen
identisch sind – **nicht**, dass jedes zukünftige Merge konfliktfrei oder
jede Wiedergabe erfolgreich ist.

## Sinnvolle Verbesserungen, gestaffelt nach Risiko

**Stufe A – sehr geringes Risiko: erst messen, keine Playback-Änderung**

- Gleiche Videos, gleiche Bildqualität und Netzwerkbedingungen auf
  1,5- und 2-GB-Geräten; kalte/warm gestartete Wiedergabe getrennt.
- Log-Phasen `MarsTubePerf` mit `adb logcat` prüfen; keine
  Accountdaten oder Video-URLs exportieren.
- RAM/PSS über `adb shell dumpsys meminfo de.equiummc.mars.smarttube`,
  zusätzlich Prozessabbruch/Low-Memory-Ereignisse und UI-Reaktionszeit.
- Videostart in Zeit bis zum **ersten gerenderten Bild** messen (nicht
  nur `STATE_READY`). Zur Kontrolle mindestens mehrere Versuche,
  P50/P95, Stream-Typ und Decoder dokumentieren.
- Grafik-/Oberflächenkosten separat vergleichen (Glas-Animationen,
  Bildauflösung und Kartenanzahl); den Look nur so weit vereinfachen,
  wie Messungen es rechtfertigen.

**Stufe B – geringes bis mittleres Risiko: isolierbare Änderungen**

- Falls Startverzögerung bei API/Formatabfragen liegt: selektive
  Optimierung des Request-Pfads oder Cache-Reuse, ohne Login/Signaturen
  oder Videoquellen zu beschädigen. Bereits vorhandenen Code vor
  Doppelimplementierung prüfen.
- Falls RAM-Druck nachweisbar ist: Geräteprofil nur für schwache Hardware
  **separat** berechnen, mit engem Hook am Player. Buffer- und
  Rückwärtspufferwerte anhand PSS und Stalls gegen den Standard testen.
  Livestreams, seek und Resume separat prüfen.
- Falls Decoderwechsel Zeit kostet: vor Änderungen Formate, Gerätecodec,
  Surface/TextureView und Zeitpunkt des ersten Frames protokollieren.
- Wenn der UI-Grafikaufwand die eigentliche Ursache ist, **UI-Effekte
  reduzieren**, statt in den Videoplayer einzugreifen.
- Neue Logik nach Möglichkeit in eigenen `Mars*`-Hilfsklassen kapseln;
  Änderungen an Upstream-Klassen klein, klar kommentiert und per Test
  abgesichert halten. Auch ein einzelner Hook muss bei Upstream-Merges
  geprüft werden.

**Stufe C – hohes Risiko: kompletter Player-/Media3-Wechsel**

- Ein aktueller Media3-ExoPlayer kann Android 8 grundsätzlich bedienen.
  Das beseitigt jedoch **nicht automatisch** nötige SmartTube-/SABR-
  Erweiterungen, Hardware-Decoderbeschränkungen oder Zeitverlust bei
  YouTube-Requests.
- Erforderlich wären Anpassungen der Engine, Custom-SABR-Quellen,
  Player-Controller und Track-Auswahl sowie bei DRM, Untertiteln,
  Livestreams, Audio-Fokus, SponsorBlock und Fernbedienungs-Seek.
- Ein solcher Wechsel ist **nur auf isoliertem Experimental-Branch**
  nach positiver Machbarkeitsanalyse und Vergleichsbenchmarks erlaubt.
  Nicht Bestandteil von MT-001 oder MT-002.

## Abnahmekriterien für jeden Testpatch

- Vergleich identische Hardware / Verbindung / Auflösung,
  abwechselnde A/B-Läufe, mit kalter und warmer Wiedergabe.
- Messwerte: Startzeit erstes Bild (Median/P95), PSS-RAM,
  Decoder/Codec, verworfene Frames, Anzahl/Strecke der Pufferstopps,
  Fehlerrate, Reaktion auf Seek/Back/Home und Resume.
- Testfälle: normales SDR-Video, 4K nur wenn Decoder unterstützt,
  60 fps nach Hardwarefähigkeit, Livestream, SABR, Untertitel,
  SponsorBlock, WLAN-Schwankungen, Wiedergabe nach Standby.
- **Keine Freigabe**, wenn sich Startzeit verbessert, aber
  Wiedergabestabilität, RAM-Druck oder Bedienbarkeit verschlechtern.
- Vor Freigabe testweise **neue Upstream-Stable-Version mergen**
  und sicherstellen, dass Player-Integrationen erhalten bleiben;
  unveränderte Upstream-Komponenten sind grundsätzlich vorzuziehen.
- Test-APK muss auf Android 8/API 26 installierbar bleiben; eine
  Debug-APK ist nicht automatisch signaturkompatibel zu bestehenden
  Installationen.

## Aktuelle Entscheidung

**Keine Änderung am Player-Kern, keine automatische Aktivierung eines
anderen Puffermodus und keine ungeprüfte Media3-Migration.**

Zuerst MT-001 auf freigegebenen Stand bringen, MT-002 für verlässliche
Updates absichern, dann MT-004 mit Mess-Baseline und **einer**
reversiblen Verbesserung nach der anderen bearbeiten.
