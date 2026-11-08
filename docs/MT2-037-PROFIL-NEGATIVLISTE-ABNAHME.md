# MarsTube 0.0.37 – 100.000 Ablehnungen pro YouTube-Profil, für Shorts und normale Videos

## Verbindliche Funktion
- Jedes angemeldete YouTube-Profil besitzt einen eigenen lokalen SQLite-Index
  mit **bis zu 100.000 Video-IDs**. Der anonyme Benutzer erhält einen separaten
  Index. Dieselbe Negativliste gilt profilintern für normale Videos und Shorts.
- Profilkennungen werden aus der bestehenden Account-ID **und dem aktiven
  Kanalnamen** abgeleitet und ausschließlich als SHA-256-Schlüssel gespeichert.
  Wenn ein Kanal gar keine eindeutige Kennung liefert, muss die Kontozuordnung
  beim Fire-TV-Test geprüft werden. Es wird keine zusätzliche Anmeldung erfunden.
- „Nicht interessiert“ ist bei Shorts **erste Aktion rechts oben**, danach
  Weiter/Wiederholen. Die Reihenfolge der D-Pad-Aktionen und Fokusmarkierungen
  entspricht exakt der sichtbaren Reihenfolge.
- Normale Videokarten besitzen ein eigenständiges X rechts oben auf der Vorschau;
  es startet das Video nicht. Der abgelehnte Eintrag verschwindet direkt aus der
  aktuellen Auswahl, der Fokus bleibt möglichst auf der nächsten Kachel.
- Beim normalen Player steht „Nicht interessiert“ als erste Aktion in „Mehr“.
- Die Einträge gelten für den lokal gespeicherten Video-ID-Filter auch bei
  künftig geladenen Übersichten, unabhängig davon, ob YouTube angemeldet ist.
  Serverseitiges YouTube-Feedback wird nur bei vorhandenem echten Token versucht,
  niemals als gesichert behauptet.

## Migration und Speicher
- Die bisherige globale 0.0.36-Liste von max. 1.000 Einträgen bleibt als
  Fallback-Datenquelle unangetastet. Beim ersten Start der 0.0.37 werden diese
  IDs **einmalig** in das Profil übernommen, das während der Migration aktiv ist.
  Die 0.0.36 konnte nicht speichern, zu welchem Profil die alten Ablehnungen
  ursprünglich gehörten; eine rückwirkende Verteilung wäre erfunden.
- Die neuen Daten liegen nur in der App-internen SQLite-Datenbank
  `mars_video_rejections_037.db`. Es werden lediglich 250 aktuelle IDs je
  SQL-Abfrage geprüft; nicht alle 100.000 im RAM vorgehalten.
- Die DB ist `UNIQUE(profile_key,video_id)` indexiert und löscht bei
  Überschreiten von 100.000 **nur den ältesten Datensatz desselben Profils**.
  Eine erneute Ablehnung aktualisiert dessen Position. Andere Profil-Listen
  bleiben vollständig erhalten.
- Gewöhnliche App-Updates mit `adb install -r` bewahren die Datenbank.
  Manuelle Datenlöschung/Deinstallation entfernt dagegen auch diese Daten.

## Fire-TV-Abnahme
1. Profil A: normales Video auf Startseite per X entfernen, Kachel verschwindet,
   unmittelbare nächste Empfehlung ist noch fokussierbar.
2. Profil A: dasselbe Video auch als Short nicht wieder anzeigen; Shorts-X oben
   mit OK auswählen → nächstes Short, Player bleibt stabil.
3. Profil B: von Profil A abgelehntes Video ist **nicht automatisch gesperrt**.
   Eine eigene Ablehnung in B darf nicht die Liste von A verändern.
4. Anonym: eigene Liste, keine YouTube-Server-Rückmeldung behaupten.
5. App-Neustart: beide Profil-Listen erhalten, alte 0.0.36-Einträge beim ersten
   Migrationseintritt erhalten, keine erneute Migration beim Profilwechsel.
6. Bei 100.001 Ablehnungen in einem Profil verschwindet exakt der älteste
   Eintrag dieses Profils; die andere Liste bleibt unverändert.
7. Keine sichtbare UI-Pause bei einem X-Druck oder schnellem Blättern durch
   viele Videokacheln; kein 100.000-Elemente-Set im Shorts-Player.
8. Weiter/Wiederholen und Shorts-Discovery weiterhin erfolgreich.
9. Fire TV Cube Update über vorhandene App mit identischer Signatur
   und unveränderter Paket-ID `de.equiummc.marstube2`.

## Noch getrennt zu implementieren
- Eine sichtbar integrierte YouTube-Profil-Auswahlliste mit drei Profilen
  und vollständigem Konto-/Kanalwechsel in der TV-Oberfläche.
- Live und Musik als eigenständige linksseitige Navigationseinträge.
Diese ursprünglich benannten Wünsche sind **nicht** durch die Negativlisten-Änderung erledigt.
