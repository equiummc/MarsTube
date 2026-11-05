# MT2-017 — Wiedergabeausfall auf Fire TV / Thomson

Stand: 06.11.2026. **Diagnose offen — keine Ursache als bewiesen ausgeben.**

## Beobachtungen vom Anwender

- MarsTube 1 (SmartTube-basierter Fork) und MarsTube 2 (Media3/NewTube) können aktuell YouTube-Videos auf TV-Geräten nicht starten.
- Vorschaubilder können geladen werden; das beweist keinen funktionierenden Videostream.
- PC-Browser kann YouTube-Videos abspielen. Damit ist ein allgemeiner YouTube-Komplettausfall unwahrscheinlich, aber TV-Client-/Netzwerk-/Tokenfehler bleiben möglich.
- Der Anwender hat in MarsTube 2 **„SABR bevorzugen“ aktiviert** und denselben Wiedergabefehler beobachtet. Den Schalter nicht erneut als Lösung vorschlagen.

## Quellcodebefund

- Die NewTube-Referenz ist auf SHA `6f96a1b42bb9272935360d7e5aec34d35f2753f0` gepinnt.
- In `SabrSourcePreference` ist die SABR-Präferenz standardmäßig aus. `SabrSourcePreference.isFallbackEnabled` liefert im Release grundsätzlich `false`.
- Laut Kommentar in dieser Referenz wurde SABR-only-Fallback ohne gültige Stream-Autorisierung bewusst deaktiviert: Testvideos stoppten nach ungefähr einer Minute wegen `ATTESTATION_REQUIRED`.
- Unser `patch_sabr_recovery.py` macht lediglich einen begrenzten *SABR -> DASH*-Wechsel, wenn dieselbe gültige Antwort DASH-URLs enthält; kein Nachweis einer vollständigen Problemlösung.
- Die SmartTube-Beta 32.70 enthält `prefer_sabr_live`, das im `VideoLoaderController` nur bei `formatInfo.isLive()` verwendet wird: kein direkter Fix für gewöhnliche VOD-Videos.
- **Nicht** wahllos SmartTube-Patches auf das unabhängige Media3/SABR-System übertragen.

## Nächste Schritte — ohne neue Test-App

1. Fire TV nach einem reproduzierten Fehler via ADB diagnostizieren, relevante `logcat`-Zeilen auf Quelle/Token/HTTP/Engine beschränken und sensible URLs/Token entfernen.
2. Trennen, ob `/player` keine gültige Antwort liefert, SABR bei fehlender Autorisierung scheitert, DASH-URLs 403/410 liefern, oder Media3/Decoder den Stream nicht öffnen kann.
3. Nur den belegten Fall beheben: begrenzte Client-Neuauswahl bei ungültiger Antwort oder kompatible Quelle/Fallback mit max. einem Wiederholungsversuch pro Video. Keine Umgehung von Schutz- oder Autorisierungsmechanismen.
4. Fehlerklasse für TV sichtbar machen; keine endlose Ladeanimation und kein automatischer Endloswechsel SABR/DASH.
5. Nur über die bestehende App-ID `de.equiummc.marstube2` mit derselben Release-Signatur als **0.0.17** ausliefern; zuerst GitHub-Build, danach der reale OTA-Test mit dem Besitzer. Niemals ein neues `firetest`-Paket erzeugen.

## Status

Fehler reproduziert laut Nutzer, technische Logursache noch nicht erhoben. Produktivcode der 0.0.16 bleibt unverändert.
