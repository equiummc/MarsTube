# MT2-024 – UI/UX-Audit der TV-Navigation (07.11.2026)

**Status:** Codeprüfung und isolierter Entwicklungsbranch; **keine Gerätefreigabe und keine veröffentlichte 0.0.24-APK**.

**Basis:** offiziell veröffentlichte MarsTube 0.0.23, gepinnte NewTube-Buildkette. Die produktive Paketkennung de.equiummc.marstube2, Media3, SABR und die bestehende OTA-/Signaturprüfung bleiben unverändert.

## Angesetzte Skills und Grenzen

- Emil Kowalski: emil-design-eng — Fokus/Bedienung sofort, Feedback präzise, Animationen kurz und begründet.
- apple-design — räumliche Konsistenz, eindeutige Orientierung, unterbrechbare Bewegung und sofortige Rückmeldung.
- break-ui / review-animations — Tests mit langen Texten, unsichtbaren/fehlenden Elementen, schnellen D-Pad-Folgen und knappen Ressourcen.
- Auf dem Windows-Rechner des Nutzers liegen zusätzlich OpenCode-Skills mars-unslop-ui, mars-unslop-code, mars-unslop-content und der Befehl /mars-quality. Die lokalen Skills-Dateien sind über diese GitHub-Verbindung nicht direkt les-/ausführbar. Dieser Bericht ist deshalb ein Quellcode-Audit unter Anwendung öffentlicher Kriterien, kein ausgeführter lokaler Skill-Lauf.

## Vorher/Nachher-Prüfung

| Vorher | Neu in 0.0.24 (Branch) | Begründung |
| --- | --- | --- |
| MarsTvRail und MarsTvShell definieren ihre Menüeinträge getrennt | MarsTvNavigationModel stellt die sieben Hauptziele bereit | Keine abweichenden Menüfolgen |
| M ist oben nur Dekoration, darunter Start mit Haus | Das M selbst wird zum fokussierbaren Startseitenknopf | Ein zentraler Startpunkt |
| In Ausgangsdateien steht 0.0.15, zusätzliche Versionszeile auf der Rail | Versionszeile wird vollständig aus der Rail entfernt | Keine historische Versionsanzeige bei Shorts |
| Browse stoppt an der ersten/letzten Zeile; Player läuft im Kreis | Einheitliches zyklisches wrap für Hoch/Runter | Kürzere Navigation |
| Settings-Leiste 132 dp, andere 64 dp | Einheitliche 64 dp | Mehr Bildschirmfläche und visuelle Konsistenz |
| Beschriftungen fehlen bei schmaler Leiste | Gemeinsames nichtfokussierbares Label rechts, 125 ms Eintritt | Orientierung ohne Layoutsprung |
| Shorts hat zusätzlichen globalen Menüpunkt Shorts Auto | Funktion bleibt in Shorts-Bedienelementen, nicht in Hauptleiste | Sieben gleiche Hauptziele |
| Jeder Fokuswechsel zeichnet unnötig Teile neu | Flyout bewegt sich über Translation ohne wiederholten Layout-Pass | Niedrige Kosten auf Fire TV und Thomson |

## Festgelegte Reihenfolge

1. M – Startseite
2. Shorts
3. Suche
4. Abos
5. Verlauf
6. Profil
7. Einstellungen

Ein **nur bei aktivem Miniplayer** sichtbarer Schnellzugang Laufendes Video bleibt vorerst als kontextuelle Ausnahme. Wenn er verschwindet, muss der Fokus auf einem gültigen Ziel landen.

## Verbindliches Verhalten

- Hoch von M führt zu Einstellungen; Runter von Einstellungen führt zu M. D-Pad-Fokus wechselt ohne Verzögerung.
- BACK im Player öffnet weiter die Navigation, ohne Wiedergabeunterbrechung. BACK in Unter-Einstellungen geht weiterhin eine Seite zurück.
- Rechts vom Menü führt in den zuletzt sinnvollen Inhaltsfokus; nach einem Scroll-/Seitenwechsel kein Fokus auf unsichtbare Karte.
- M steht als einfarbige Markenform oben, mit mattem blauen Fokusakzent.
- Das kurze rechts herausfahrende Label animiert nur bei erstem Auftauchen. Wiederholtes D-Pad wechselt Label und Position sofort.
- Kein Live-Blur, keine zusätzliche Videodekoderinstanz, keine teuren Effekte.

## Vor Veröffentlichung testen

- [ ] Enden nach oben und unten in allen TV-Kontexten umlaufend.
- [ ] Identische sieben Ziele in Home, Suche, Einstellungen, Player und Shorts.
- [ ] Kein unsichtbarer achter Fokuspunkt in normalen Videos oder Shorts.
- [ ] Shorts-Automatik bleibt direkt in der Shorts-Ansicht auswählbar.
- [ ] Miniplayer-Schnellzugang nur bei tatsächlich aktiver Wiedergabe.
- [ ] BACK im Player öffnet Navigation, Wiedergabe läuft weiter.
- [ ] BACK in Unter-Einstellungen kehrt zur übergeordneten Seite zurück.
- [ ] Rechts aus der Rail stellt den korrekten Inhaltsfokus wieder her.
- [ ] Auf dem Bildschirm steht keine alte Versionskennung 0.0.15.
- [ ] Lange Menütitel, geringe Auflösung und schnelle Tastendrücke erzeugen kein Flackern oder Fokusverlust.
- [x] Android-Debug-Build + JUnit + CI-Prüfungen der bestehenden Update-Routen grün: [Run 42369214983](https://github.com/equiummc/MarsTube/actions/runs/42369214983). Produktive APK-Signatur und Installation wurden in diesem Branch nicht getestet; das ist keine Releasefreigabe.
- [ ] Praktischer Fire-TV-Test, danach schwacher Thomson-TV.

## Dateien im Feature-Branch

- mt2-player-lab/MarsTvNavigationModel.java – gemeinsame Menü-IDs und Wrap-Logik.
- mt2-player-lab/MarsTvRail024.java – feste schmale Navigation und Fokus.
- mt2-player-lab/MarsTvRailLabel.java – rechts ausfahrendes, leichtgewichtiges Label.
- mt2-player-lab/patch_024_unified_navigation.py – Post-0.0.23-Patch für Player und UI-Theme.
- mt2-player-lab/MarsTvNavigationModelTest.java – JUnit-Tests.
- tools/build-marstube-production.sh – Buildreihenfolge bis 024.
- .github/workflows/mt2-024-navigation-ci.yml – CI ohne Veröffentlichung.

## Offene Risiken

Quellcodeprüfung ersetzt keinen Gerätetest. Insbesondere müssen wir das Flyout beim ersten Öffnen, alte Activity-Fokusreferenzen, dynamische Miniplayer-Einträge, Barrierefreiheit und echte Speicher-/Frameratewerte untersuchen. Der Fehler ist konkret auf die Patch-Reihenfolge zurückzuführen: MT2-016 ersetzt die Player-Version durch BuildConfig, danach überschreibt MT2-021 die komplette MarsTvShell.java wieder mit dem statischen Stand 0.0.15. MT2-023 ändert nur das Logo. MT2-024 entfernt deshalb das statische Versionslabel am Ende der gesamten Patchkette und prüft dessen Abwesenheit. Welche APK bereits am Fire TV läuft, bleibt separat anhand eines Gerätetests zu bestätigen.

**Release-Gate:** Nicht als 0.0.24 veröffentlichen, bevor vollständiger Build, Unit-Tests und reale Fokus-/BACK-/Shorts-Geräteprüfung vorliegen.
