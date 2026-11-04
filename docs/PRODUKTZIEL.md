# MarsTube – verbindliches Produktziel

**Stand:** 04.11.2026 · **Gilt für:** MarsTube 0.0.10 und alle folgenden Versionen · **Projekt:** `equiummc/MarsTube`

## Leitbild

**MarsTube soll schlank, schnell, hochwertig und angenehm zu bedienen sein.** Ein moderner, optisch ansprechender Player, der auf schwachen Geräten flüssig bleibt und auf leistungsfähigen Geräten die bestmögliche verfügbare Bild- und Tonqualität ausschöpft. Nicht die kleinste Auflösung gewinnt, sondern das beste Zusammenspiel von Qualität, Geschwindigkeit, Stabilität, Gerät und Verbindung.

## Verbindliche Produktprinzipien

1. **Bildqualität ohne künstliche Obergrenze.** Kein pauschales 1080p-Limit. Wenn Quelle, Bildschirm, Decoder, Bandbreite und Streamingformat es zulassen, soll MarsTube auch **4K/UHD**, **60 FPS**, **HDR** und hochwertige Codecs nutzen. Auch höhere Formate werden nicht grundsätzlich ausgeschlossen, sofern alle Beteiligten sie tatsächlich unterstützen.
2. **Intelligente Automatik statt technischer Pflichtauswahl.** Ab Werk bevorzugt MarsTube automatisch die beste *stabil abspielbare* Qualität, die für Quelle und Gerät verfügbar ist. Hardwarefähigkeiten, Auflösung, FPS, HDR, Decoderprofile, Codec-Unterstützung und Bandbreite werden berücksichtigt. Wenn etwas nicht verfügbar oder zu anspruchsvoll ist, erfolgt ein sinnvoller Fallback.
3. **Manuelle Kontrolle bleibt möglich.** Auflösung, FPS, Codec-Präferenz, HDR, Streaming-Protokoll und Puffer sind konfigurierbar. Für die Videobitrate gibt es „Automatisch / kein festes Limit“ sowie optionale Obergrenzen (z. B. 3, 5, 10, 15, 25 Mbit/s). **Ab Werk kein festes 10-Mbit/s-Limit.** Eine Begrenzung dient dem Sparen von Daten oder schwachen Verbindungen, nicht der generellen Qualitätspolitik. Eine Bitratenobergrenze steuert die Streamauswahl, sie transkodiert den Stream nicht.
4. **Schneller, sauberer Videostart.** Videos beginnen zügig, ohne sichtbares horizontales Strecken eines Shorts, ohne unscharfe Vorschaumatsche und ohne unnötige Wechsel zwischen falschem und richtigem Seitenverhältnis. Wenn nötig, lieber einen sehr kurzen stabilen Übergang als ein sichtbar schlechtes Bild. Niemals „scharf“ durch künstliches Upscaling vortäuschen. Schneller Start darf die eigentliche Wiedergabequalität nicht dauerhaft herabsetzen.
5. **Ressourcenschonend auf TV-Hardware.** Auch Thomson (~1,45 GB RAM) und Fire TV (~2 GB RAM) sollen flüssig bleiben. Leichte Farbverläufe, Android-Vektorgrafiken und schlanke Layouts statt teurer dauerhafter Blur-/Glow-Animationen oder zusätzlicher Videodecoder für Dekoration. Speicher, Puffer, Netzwerk und CPU/GPU bleiben kontrolliert.
6. **Stabile und nachvollziehbare Wiedergabe.** Media3 als Playerbasis; SABR optional ein-/ausschaltbar und nur als tatsächlich verwendetes Protokoll melden. Bei Fehlern nachvollziehbarer Fallback. Keine schwarzen Screens bei Shorts-Wechsel, keine blockierten Menüs, kein kaputter Ton bei temporärer 2×-Wiedergabe.
7. **Hochwertiges, verbindliches Erscheinungsbild.** Das vom Nutzer am 04.11.2026 freigegebene **neonblaue MarsTube-Player-Zielbild** ist die visuelle Referenz: dunkles Blau, transparente Bedienleiste mit Farbverlauf, gut lesbare große Fortschrittsanzeige, scharf skalierte Videokacheln, M-Logo, konsistente moderne Weiß/Cyan-Icons mit dezenten Akzenten und abgerundete Fokusrahmen. Kein neuer konkurrierender Designentwurf. Effekte so leichtgewichtig wie möglich nachbauen. Grafikdatei zusätzlich unter `design/reference/marstube-ui-target.png` ablegen und im Build-Review heranziehen.
8. **Einfach zu bedienen.** Sämtliche Funktionen und Untermenüs sind mit TV-Fernbedienung erreichbar. BACK öffnet während Wiedergabe die Navigation, ohne die Wiedergabe unnötig zu stoppen. Getrennte, passende Bedienkonzepte für Android TV/Fire TV und perspektivisch Smartphone in Hoch-/Querformat.
9. **Einstellungen verständlich bündeln.** Ein zentraler Bereich „Video & Wiedergabe“ für Qualität, Bitrate, FPS, Codec, HDR, Puffer und Streaming; weitere Bereiche für Darstellung, Fernbedienung, Untertitel und Profil. Keine doppelten oder nicht erreichbaren Einstellungen. TV-Downloads nicht anzeigen; Smartphone-Unterstützung separat planen.
10. **Qualität vor Showeffekt und vor Versionsnummern.** Ein Build ist erst ein technischer Zwischenstand. Freigabe erst nach Navigationstests, Video- und Shorts-Wiedergabe, Bildqualitätsprüfung und echten Tests auf Fire TV und Thomson. Bestehende stabile Installationen bleiben bis dahin unverändert.

## Prüfkriterien für jede neue Version

- **Bildqualität:** Auf geeignetem 4K/HDR-Gerät und passendem Quellvideo kann tatsächlich ein kompatibler 4K/HDR-Track gewählt und wiedergegeben werden; auf einem 1080p-Gerät wird kein 4K-Versprechen abgegeben.
- **Automatische Qualität:** Kein hart kodiertes 1080p-, 10-Mbit/s- oder 30-FPS-Standardlimit. Bei unzureichender Bandbreite/Decoderfähigkeit sinkt die Qualitätswahl angemessen, ohne dauerhaft zu verharren.
- **Videostart und Wechsel:** Zeiten bis zum ersten scharfen Bild und Unterbrechungen messen; keine verzerrten Shorts oder unnötigen Schwarzbilder. Performance gegenüber 0.0.9 auf Thomson/Fire TV vergleichen.
- **Streaming und Diagnose:** Tatsächlich gewählte Auflösung/FPS/Codec/HDR/Bitrate und aktives Streamingverfahren sind im Info-Bereich erkennbar, nicht dauerhaft auf das Video eingeblendet.
- **Oberfläche:** Reale Screenshots von Startseite, Player, Shorts, Einstellungen und Untermenüs mit dem freigegebenen Zielbild und den D-Pad-Anforderungen vergleichen.
- **Stabilität:** Kein Freeze in Untermenüs, zuverlässiges Back und OK-Hold, keine Audioaussetzer oder Speicherprobleme durch Dekoration.

## Arbeitsregel für OpenCode und Reviews

Diese Produktziele sind der Maßstab für technische Entscheidungen. **Keine Abkürzung über einen pauschalen Qualitätsdeckel, keine kosmetischen Änderungen auf Kosten der Startzeit und keine neuen Optionen, die nur angezeigt werden, aber nichts tun.** Bei Zielkonflikten den Trade-off dokumentieren und anhand realer Tests entscheiden.

Aktive Umsetzung: `mt2-player-lab/MT2-010-ARBEITSAUFTRAG.md`.
