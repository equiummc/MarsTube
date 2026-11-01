# MarsTube: Verbindlicher Entwicklungs- und Installationsstandard

Diese Regeln gelten fuer **jede** MarsTube-Aenderung und jede KI/Codex-Arbeit im Repository. Insbesondere gelten sie fuer Fire TV Cube (`raven` / `192.168.41.24:5555`) und Thomson Android TV (`nagata` / `192.168.41.53:5555`). IP-Adressen muessen vor jeder Installation ueber ADB-Geraeteidentitaet bestaetigt werden.

## 1. Ursache der wiederholten Installationsabbrueche (MT-020 bis MT-022)

Ein erfolgreich gebautes APK beweist **nicht**, dass das PowerShell-Installationsskript funktioniert. Bei MT-022 enthielten beide Installer noch eine MT-021-Schutzbedingung, die mit dem MT-022-Quelltext unvereinbar war. Die CI hatte zunaechst lediglich Stichwoerter und Syntax geprueft; die echte Guard-Auswertung auf dem gebauten Quellstand nicht. Der erste TV-Installationsversuch schlug daher nach dem Backup und vor `adb install` fehl. Zusaetzlich wurden alte festgepinnte Script-Commit-IDs in bereits ausgeteilten Befehlen wiederverwendet.

**Diese Klasse von Fehlern muss vor Herausgabe eines Installationsbefehls abgefangen werden.**

## 2. Verbindliche Freigabekriterien (alle Pflicht)

1. **Immer zuerst den exakten Stand definieren:** Feature-Branch, *vollstaendiger* `sourceSha`, *vollstaendiger* `installerSha`, Android-Paket `de.equiummc.mars.smarttube`, erwartete Zielgeraete. Code und Installer duerfen auf verschiedene SHAs gepinnt sein, aber nur bewusst und mit automatisierter Kompatibilitaetspruefung. Niemals stillschweigend einen alten Script-Commit wiederverwenden.
2. **Zwei getrennte Installationsskripte und PowerShell-Bloecke:** einer nur fuer Fire TV (`raven`/AFTR), einer nur fuer Thomson (`nagata`/Thomson). Keinen kombinierten Geraetewahlschalter ausgeben, wenn Einzelbefehle gewuenscht sind.
3. **Gleiche Quellen wie auf dem Windows-PC:** CI muss die exakt gepinnte Quellrevision isoliert auschecken und dieselben Patch-/Overlay-Schritte sowie Guard-Pruefungen durchlaufen, die der Installer ausfuehrt. Keine nur aehnlichen Ersatzpruefungen.
4. **Installationsskript tatsaechlich pruefen:** PowerShell-AST/Parser fuer beide Dateien **und** jede Vorinstallations-Pruefung gegen die tatsaechliche zu installierende Quellrevision ausfuehren (`VerifyOnly`/Dry-Run oder isolierter aus dem Originalskript entnommener Guard-Block). Der positive Fall muss bestehen; ein absichtlich verletzter Guard muss blockieren. Ein einfacher `Contains`-/`grep`-Test reicht nicht.
5. **Build vor Auslieferung:** Unit-Tests, vorhandene Overlay-`--check`-Schritte, gebrandetes Universal-APK und vollstaendiger CI-Job muessen gruen sein. Kein PowerShell-Installationsbefehl bei `queued`, `in_progress`, `failure` oder unbekanntem Status.
6. **Vor `adb install -r`:** per `getprop ro.product.device`, Modell, Paket-ID, APK-`versionCode`, `apksigner`-SHA256 und Versionsvergleich verifizieren; laufende APK zuvor in separatem Zeitstempelordner sichern. Kein `pm clear`, `pm uninstall`, keine Aenderung von HOME/Launcher, keine geloeschten Benutzerdaten. Jeder Fehler bricht **vor** Installation ab.
7. **End-to-End-Skriptvertrag:** Das automatisch getestete Script ist genau das, dessen Commit-SHA im ausgegebenen PowerShell-Befehl steht. CI pruft diesen SHA, Guard-Ausfuehrung, Zielgeraet, Paket-Signatur-Pfad und Korrektheit der Verweigerung. Aenderungen am Installer erfordern neue Freigabe und neue PowerShell-Befehle.
8. **Kommunikation:** Fuer Fire und Thomson jeweils einen vollstaendigen, direkt kopierbaren PowerShell-Block liefern. Exakte getestete Revisionen und CI-Run nennen. Erfolg einer CI **nicht** als erfolgreichen physischen Geraetetest verkaufen; Shorts-Start, Vor-/Zurueckwechsel und schwarze Frames muessen auf den Fernsehern separat abgenommen werden.
9. **Stabilitaet vor Beschleunigung:** Bestehende gut funktionierende Long-Video-Starts und Shorts-Navigation nicht unnoetig umbauen; experimentelles Prefetch nur isoliert, reversibel und mit A/B-Test. Bei Regression sichere Rueckkehr zur vorherigen signierten APK ermoeglichen.
10. **Fehlerbericht = Pflicht zur Prozessverbesserung:** Bei Installationsabbruch erst unterscheiden, ob der Guard **zu Recht** blockiert oder veraltet ist. CI-Test um die konkrete Fehlerursache erweitern, **bevor** erneut ein Befehl herausgegeben wird. Niemals dem Anwender empfehlen, Schutzchecks zu entfernen oder an ihnen vorbeizuarbeiten.

## 3. Abnahme-Checkliste vor jeder Nutzeranweisung

- [ ] Geraeteidentitaet Fire und Thomson exakt bekannt
- [ ] ScriptSHA und sourceSha explizit gepinnt und wechselseitig geprueft
- [ ] Beide Installer syntaktisch gueltig
- [ ] **Alle** tatsaechlichen Vorinstallations-Guards auf gepinntem Quellstand **ausgefuehrt**, positiv/negativ
- [ ] Identische Overlays angewendet und geprueft
- [ ] Vollstaendige Android-Unit-Tests und Universal-APK-Build erfolgreich
- [ ] APK-Signatur- und Backup-Sicherheitspruefungen vorhanden
- [ ] CI fuer exakt auszugebenden Installer-Commit vollstaendig `success`
- [ ] Je TV ein eigenstaendiger PowerShell-Block ausgegeben
- [ ] Physische TV-Funktion als noch ungetestet bezeichnet, solange keine reale Abnahme vorliegt

**Falls irgendein Punkt fehlt: keinen Installationsbefehl als installierbereit ausgeben.**
