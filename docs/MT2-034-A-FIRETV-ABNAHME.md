# MT2-034 A – Player- und Menüführung (Fire TV Cube)

Basis: signierte MT2-033, eine unveränderte Paketkennung `de.equiummc.marstube2`.
Isolierter Build aus gepinntem Upstream + vorhandener MarsTube-Patchkette.

## Verbindliches UX-Ziel
- Ein **und derselbe** laufende Media3-Player: links verkleinertes Video, rechts Kontextmenü.
- MarsTube-Blau statt YouTube-Farbkopie. Fernbedienung mit klarer Fokusmarkierung.
- Zurück: im Untermenü eine Ebene höher, erst danach Menü schließen.
- Hauptbedienung ohne unnötige Spultasten neben Play/Pause; Seek per D-Pad/Progress bleibt.
- Die Menüleiste liegt schwebend über einem dezenten blauen Verlauf. Fortschritt bleibt in separater schmaler Kapsel.

## Im Code von MT2-034 A vorbereitet
- Hierarchisches rechte TV-Panel: Kanal, Merken, Kommentare, Einstellungen, Mehr.
- In-Panel-Auswahl mit echten Formaten aus `getVideoFormats`, `getAudioFormats`, `getSubtitleFormats`, `SPEED_PRESETS`.
- Bedienung verändert die bestehende ExoPlayer-Instanz über bestehende `setFormat`, `applyCaptionFormat`, `applySpeed`-Wege.
- Videowechsel und veraltete Auswahllisten werden per `videoId` geprüft.
- Kanal abonnieren über bestehende Presenter-Aktion; keine vorgetäuschte Anmeldung.
- Kommentare: bestehendes `CommentsPanel` rechts, TV-Pane ohne zweite Streaming-Instanz.
- Eigene Version 12034, gleicher Original-Signaturschlüssel und Updatepfad.

## Explizite Grenzen (nicht als fertig ausgeben)
- Direkte Glockensteuerung: kein geprüfter Backend-Hook. Menüpunkte führen zu Kanalseite.
- `Später ansehen`: bislang über vorhandenen Playlist-Auswahldialog; kein falscher Direktstatus.
- Untertitel-Design: bestehende Einstellungsansicht wird geöffnet.
- Kommentarleiste und Kanal-Seitenwechsel müssen **auf dem Fire TV** geprüft werden:
  Surface/Reparenting, BACK, Fokus, Scrollen und Erhalt des laufenden Videos.
- Die tatsächlich sichtbare Gestaltung ist erst nach Screenshots/Device-Abnahme bestätigt.
- Keine Pixelkopie von YouTube (nur Bedienprinzip als Referenz).

## Fire-TV-Abnahmetests
1. Video wählen, Oben öffnet Controls; Fokus wählbar bei allen neun Bedientasten.
2. Kanal öffnen, Abonnement wählen, Status kontrollieren, Glocke zur Kanalseite.
3. Mehr → Einstellungen → Qualität → 720p oder Auto → Zurück → Einstellungen → Mehr.
4. Tonspur nur bei mehrsprachigem Video anzeigen; Umstellen gilt nur für das aktuelle Video.
5. Untertitel Aus/An und Sprache; Tempo 0,5×/1×/1,5×.
6. Kommentare öffnen; D-Pad scrollt im Kommentarbereich, Video läuft links weiter.
7. Merken und Später ansehen führen zu nachvollziehbaren vorhandenen Speicheraktionen.
8. Menü schließen → Player wieder voll; erneutes Öffnen ohne Fokusverlust.
9. Ein Video wechseln, während eine Auswahlliste geöffnet ist: keine Aktion auf falscher Video-ID.
10. 20+ Wechsel und Shorts-Kontrolltest; keine schwarzen Einzelbilder oder doppelte Decoder.
11. Bestehende Zurück-Taste und schnelles Shorts-Discovery unverändert.
12. Vergleich Vorher/Nachher: tatsächlich kleinere schwebende Leiste, blauer Verlauf, Fernbedienung.

## Gate
CI muss Gradle-/Media3-/TV-Tests, Paket-ID/Version/ARM32/ARM64, SHA256 und dieselbe Signatur prüfen.
Nur CI-Artefakt; keine reguläre Release oder automatische Installation.
